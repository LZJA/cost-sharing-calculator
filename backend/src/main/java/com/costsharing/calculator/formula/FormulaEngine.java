package com.costsharing.calculator.formula;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/** 按配置分摊费用。试算、入账和海报共用这个计算入口。 */
public final class FormulaEngine {
    private static final BigDecimal CENT = new BigDecimal("0.01");

    private FormulaEngine() {}

    public static List<ShareResult> calculate(CalculatorConfig config, String ruleName, BillInput input) {
        CalculatorConfigValidator.validate(config);
        if (input == null) throw new IllegalArgumentException("请填写账单费用");
        Map<String, BigDecimal> fees = FeeCatalog.amounts(config, input);
        RuleConfig rule = findRule(config, ruleName);
        Map<String, BigDecimal> amounts = new LinkedHashMap<>();
        config.getMembers().forEach(member -> amounts.put(member.getKey(), new BigDecimal("0.00")));
        Map<String, BigDecimal> remainders = new LinkedHashMap<>();
        for (StepConfig step : rule.getSteps()) {
            if ("share".equals(step.getType())) applyShare(step, config, input, fees, amounts, remainders);
            else applyTransfer(step, amounts);
        }
        List<ShareResult> results = config.getMembers().stream().map(member -> new ShareResult(
                member.getKey(), member.getName(), member.getEmoji(), amounts.get(member.getKey()))).toList();
        BigDecimal total = fees.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal distributed = amounts.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (distributed.compareTo(total) != 0) throw new IllegalArgumentException("分摊金额合计与总费用不一致，请检查规则");
        return results;
    }

    private static RuleConfig findRule(CalculatorConfig config, String name) {
        if (name == null || name.isBlank()) {
            if (config.getRules().size() == 1) return config.getRules().get(0);
            throw new IllegalArgumentException("请选择分账规则");
        }
        return config.getRules().stream().filter(rule -> name.equals(rule.getName())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("找不到规则: " + name));
    }

    private static void applyShare(StepConfig step, CalculatorConfig config, BillInput input,
                                   Map<String, BigDecimal> fees,
                                   Map<String, BigDecimal> amounts, Map<String, BigDecimal> remainders) {
        BigDecimal amount = StepSources.of(step).stream().map(source -> feeOf(source, fees, remainders))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<ShareConfig> shares = step.getShares();
        boolean usesDays = shares.stream().anyMatch(share -> CalculatorConfigValidator.usesStayDays(share.getBasis()));
        int memberCount = config.getMembers().size();
        BigDecimal commonDenominator = BigDecimal.ONE;
        if (usesDays) {
            if (input.getTotalDays() == null || input.getTotalDays() < 1) {
                throw new IllegalArgumentException("按天数比例分摊时，周期总天数必须大于 0");
            }
            if (input.getOwnerDays() == null || input.getOwnerDays() < 0 || input.getOwnerDays() > input.getTotalDays()) {
                throw new IllegalArgumentException("参与天数必须在 0 到周期总天数之间");
            }
            commonDenominator = BigDecimal.valueOf((long) input.getTotalDays() * memberCount);
        }
        // 先统一分母，保留精确比例，避免 1/3 等循环小数在半分钱边界产生误差。
        List<BigDecimal> weights = new ArrayList<>();
        for (ShareConfig share : shares) {
            BigDecimal weight = share.getWeight() == null ? BigDecimal.ONE : share.getWeight();
            if (CalculatorConfigValidator.STAY_DAYS_PER_MEMBER.equals(share.getBasis())) {
                weight = weight.multiply(BigDecimal.valueOf(input.getOwnerDays()));
            } else if (CalculatorConfigValidator.STAY_DAYS.equals(share.getBasis())) {
                weight = weight.multiply(BigDecimal.valueOf((long) input.getOwnerDays() * memberCount));
            } else {
                weight = weight.multiply(commonDenominator);
            }
            weights.add(weight);
        }
        boolean partial = Boolean.TRUE.equals(step.getPartial());
        BigDecimal sum = sum(weights);
        if (!partial && sum.signum() == 0) throw new IllegalArgumentException("完整分摊的份额合计必须大于 0");
        BigDecimal denominator = partial ? commonDenominator : sum;
        if (partial && sum.compareTo(denominator) > 0) throw new IllegalArgumentException("只取一部分时，份额合计不能超过 1");
        List<BigDecimal> parts = allocate(amount, weights, denominator, partial);
        for (int i = 0; i < shares.size(); i++) {
            String member = shares.get(i).getMember();
            amounts.put(member, amounts.get(member).add(parts.get(i)));
        }
        if (partial) {
            remainders.put(StepSources.origin(step), amount.subtract(sum(parts)));
        }
    }

    private static void applyTransfer(StepConfig step, Map<String, BigDecimal> amounts) {
        BigDecimal ratio = step.getRatio() == null ? BigDecimal.ONE : step.getRatio();
        BigDecimal moved = amounts.get(step.getFrom()).multiply(ratio).setScale(2, RoundingMode.HALF_UP);
        List<BigDecimal> weights = step.getTo().stream()
                .map(receiver -> receiver.getWeight() == null ? BigDecimal.ONE : receiver.getWeight()).toList();
        List<BigDecimal> parts = allocate(moved, weights, sum(weights), false);
        amounts.put(step.getFrom(), amounts.get(step.getFrom()).subtract(moved));
        for (int i = 0; i < step.getTo().size(); i++) {
            String member = step.getTo().get(i).getMember();
            amounts.put(member, amounts.get(member).add(parts.get(i)));
        }
    }

    /** 先向下取整到分，再按小数余数从大到小补分；余数相同按配置顺序。 */
    private static List<BigDecimal> allocate(BigDecimal amount, List<BigDecimal> weights,
                                             BigDecimal denominator, boolean partial) {
        BigDecimal target = partial ? amount.multiply(sum(weights)).divide(denominator, 2, RoundingMode.HALF_UP) : amount;
        List<BigDecimal> parts = new ArrayList<>();
        List<BigDecimal> fractions = new ArrayList<>();
        for (BigDecimal weight : weights) {
            BigDecimal[] split = amount.movePointRight(2).multiply(weight).divideAndRemainder(denominator);
            parts.add(split[0].setScale(0, RoundingMode.UNNECESSARY).movePointLeft(2));
            fractions.add(split[1]);
        }
        int cents = target.subtract(sum(parts)).movePointRight(2).intValueExact();
        List<Integer> order = IntStream.range(0, weights.size()).boxed()
                .sorted(Comparator.<Integer, BigDecimal>comparing(fractions::get).reversed()).toList();
        for (int i = 0; i < cents; i++) {
            int index = order.get(i);
            parts.set(index, parts.get(index).add(CENT));
        }
        return parts;
    }

    private static BigDecimal sum(List<BigDecimal> values) {
        return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal feeOf(String source, Map<String, BigDecimal> fees, Map<String, BigDecimal> remainders) {
        if (source.startsWith("remainder:")) return remainders.remove(source.substring("remainder:".length()));
        if ("total".equals(source)) return fees.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return fees.get(source);
    }
}
