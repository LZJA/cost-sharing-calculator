package com.costsharing.calculator.formula;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 按配置好的步骤计算每人应承担的金额。
 * 只认识 share（按份额分一项费用）和 transfer（把某人金额的一个比例转给其他人），
 * 不执行任意表达式。
 */
public final class FormulaEngine {

    private static final int MONEY_SCALE = 2;

    private FormulaEngine() {
    }

    public static List<ShareResult> calculate(CalculatorConfig config, String ruleName, BillInput input) {
        if (config == null || config.getMembers() == null || config.getMembers().isEmpty()) {
            throw new IllegalArgumentException("计算器没有配置人员");
        }
        RuleConfig rule = findRule(config, ruleName);

        Map<String, BigDecimal> amounts = new LinkedHashMap<>();
        for (MemberConfig member : config.getMembers()) {
            amounts.put(member.getKey(), BigDecimal.ZERO);
        }

        // 上一个「只取一部分」的步骤没有分完的金额，按费用来源记下，给后面的 remainder 步骤用。
        Map<String, BigDecimal> remainders = new LinkedHashMap<>();

        if (rule.getSteps() != null) {
            for (StepConfig step : rule.getSteps()) {
                applyStep(step, config, input, amounts, remainders);
            }
        }

        List<ShareResult> results = new ArrayList<>();
        for (MemberConfig member : config.getMembers()) {
            results.add(new ShareResult(
                    member.getKey(),
                    member.getName(),
                    member.getEmoji(),
                    money(amounts.get(member.getKey()))
            ));
        }
        return results;
    }

    private static RuleConfig findRule(CalculatorConfig config, String ruleName) {
        if (config.getRules() == null || config.getRules().isEmpty()) {
            throw new IllegalArgumentException("计算器没有配置规则");
        }
        if (ruleName == null || ruleName.isBlank()) {
            if (config.getRules().size() == 1) {
                return config.getRules().get(0);
            }
            throw new IllegalArgumentException("请选择分账规则");
        }
        return config.getRules().stream()
                .filter(rule -> ruleName.equals(rule.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("找不到规则: " + ruleName));
    }

    private static void applyStep(StepConfig step, CalculatorConfig config, BillInput input,
                                  Map<String, BigDecimal> amounts, Map<String, BigDecimal> remainders) {
        if (step == null || step.getType() == null) {
            throw new IllegalArgumentException("规则步骤缺少类型");
        }
        switch (step.getType()) {
            case "share" -> applyShare(step, config, input, amounts, remainders);
            case "transfer" -> applyTransfer(step, amounts);
            default -> throw new IllegalArgumentException("不认识的规则步骤: " + step.getType());
        }
    }

    private static void applyShare(StepConfig step, CalculatorConfig config, BillInput input,
                                   Map<String, BigDecimal> amounts, Map<String, BigDecimal> remainders) {
        BigDecimal fee = feeOf(step.getSource(), input, remainders);
        List<ShareConfig> shares = step.getShares();
        if (shares == null || shares.isEmpty()) {
            throw new IllegalArgumentException("share 步骤没有分配对象");
        }

        BigDecimal weightSum = BigDecimal.ZERO;
        List<BigDecimal> weights = new ArrayList<>();
        for (ShareConfig share : shares) {
            requireMember(config, share.getMember());
            BigDecimal weight = weightOf(share, input);
            weights.add(weight);
            weightSum = weightSum.add(weight);
        }
        if (weightSum.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("share 步骤的份额合计为 0");
        }

        // partial：份额是占费用的绝对比例，合计可以小于 1，剩下的留给后面的步骤。
        // 否则按份额归一，把这项费用全部分完。
        BigDecimal denominator = Boolean.TRUE.equals(step.getPartial()) ? BigDecimal.ONE : weightSum;
        if (Boolean.TRUE.equals(step.getPartial()) && weightSum.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("只取一部分时，份额合计不能超过 1");
        }

        BigDecimal distributed = BigDecimal.ZERO;
        for (int i = 0; i < shares.size(); i++) {
            BigDecimal part = money(fee.multiply(weights.get(i)).divide(denominator, 8, RoundingMode.HALF_UP));
            distributed = distributed.add(part);
            String member = shares.get(i).getMember();
            amounts.put(member, amounts.get(member).add(part));
        }
        if (Boolean.TRUE.equals(step.getPartial())) {
            remainders.put(step.getSource(), money(fee.subtract(distributed)));
        }
    }

    private static void applyTransfer(StepConfig step, Map<String, BigDecimal> amounts) {
        if (!amounts.containsKey(step.getFrom())) {
            throw new IllegalArgumentException("transfer 的转出人不存在: " + step.getFrom());
        }
        List<ShareConfig> receivers = step.getTo();
        if (receivers == null || receivers.isEmpty()) {
            throw new IllegalArgumentException("transfer 步骤没有接收人");
        }

        BigDecimal ratio = step.getRatio() == null ? BigDecimal.ONE : step.getRatio();
        BigDecimal moved = money(amounts.get(step.getFrom()).multiply(ratio));
        amounts.put(step.getFrom(), amounts.get(step.getFrom()).subtract(moved));

        BigDecimal weightSum = BigDecimal.ZERO;
        for (ShareConfig receiver : receivers) {
            if (!amounts.containsKey(receiver.getMember())) {
                throw new IllegalArgumentException("transfer 的接收人不存在: " + receiver.getMember());
            }
            weightSum = weightSum.add(receiver.getWeight() == null ? BigDecimal.ONE : receiver.getWeight());
        }
        if (weightSum.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("transfer 步骤的接收份额合计为 0");
        }

        for (ShareConfig receiver : receivers) {
            BigDecimal weight = receiver.getWeight() == null ? BigDecimal.ONE : receiver.getWeight();
            BigDecimal part = money(moved.multiply(weight).divide(weightSum, 8, RoundingMode.HALF_UP));
            amounts.put(receiver.getMember(), amounts.get(receiver.getMember()).add(part));
        }
    }

    private static BigDecimal feeOf(String source, BillInput input, Map<String, BigDecimal> remainders) {
        if (source == null) {
            throw new IllegalArgumentException("share 步骤缺少费用来源");
        }
        if (source.startsWith("remainder:")) {
            String origin = source.substring("remainder:".length());
            BigDecimal remainder = remainders.get(origin);
            if (remainder == null) {
                throw new IllegalArgumentException("没有找到上一步剩下的「" + origin + "」，请先加一步只取一部分的分摊");
            }
            return remainder;
        }
        return switch (source) {
            case "water" -> zero(input.getWaterBill());
            case "electric" -> zero(input.getElectricBill());
            case "gas" -> zero(input.getGasBill());
            case "total" -> zero(input.getWaterBill()).add(zero(input.getElectricBill())).add(zero(input.getGasBill()));
            default -> throw new IllegalArgumentException("不认识的费用来源: " + source);
        };
    }

    /**
     * 份额：固定 weight，或 weight = days/totalDays（某人居住天数占当月总天数）。
     */
    private static BigDecimal weightOf(ShareConfig share, BillInput input) {
        if ("days/totalDays".equals(share.getBasis())) {
            int totalDays = input.getTotalDays() == null ? 0 : input.getTotalDays();
            int ownerDays = input.getOwnerDays() == null ? 0 : input.getOwnerDays();
            if (totalDays <= 0) {
                throw new IllegalArgumentException("使用居住天数分摊时，总天数必须大于 0");
            }
            if (ownerDays < 0 || ownerDays > totalDays) {
                throw new IllegalArgumentException("居住天数必须在 0 到总天数之间");
            }
            BigDecimal ratio = new BigDecimal(ownerDays)
                    .divide(new BigDecimal(totalDays), 8, RoundingMode.HALF_UP);
            BigDecimal weight = share.getWeight() == null ? BigDecimal.ONE : share.getWeight();
            return ratio.multiply(weight);
        }
        if (share.getWeight() == null) {
            throw new IllegalArgumentException("份额缺少 weight");
        }
        return share.getWeight();
    }

    private static void requireMember(CalculatorConfig config, String key) {
        boolean exists = config.getMembers().stream().anyMatch(member -> member.getKey().equals(key));
        if (!exists) {
            throw new IllegalArgumentException("规则引用了不存在的人员: " + key);
        }
    }

    private static BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static BigDecimal money(BigDecimal value) {
        return zero(value).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
