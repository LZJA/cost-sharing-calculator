package com.costsharing.calculator.formula;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 校验整条规则的费用流向，避免重复计费、漏分和无法填写的输入。 */
public final class CalculatorConfigValidator {
    public static final String STAY_DAYS = "days/totalDays";
    public static final String STAY_DAYS_PER_MEMBER = "days/totalDays/memberCount";
    private static final Set<String> FEES = Set.of("water", "electric", "gas");

    private CalculatorConfigValidator() {}

    public static boolean usesStayDays(String basis) {
        return STAY_DAYS.equals(basis) || STAY_DAYS_PER_MEMBER.equals(basis);
    }

    public static void validate(CalculatorConfig config) {
        require(config != null && config.getMembers() != null && config.getMembers().size() >= 2,
                "至少需要两名人员");
        Set<String> keys = new HashSet<>();
        for (MemberConfig member : config.getMembers()) {
            require(member != null && member.getKey() != null && !member.getKey().isBlank(), "人员缺少标识");
            require(member.getName() != null && !member.getName().isBlank(), "人员名字不能为空");
            require(keys.add(member.getKey()), "人员标识重复: " + member.getKey());
        }
        require(config.getRules() != null && !config.getRules().isEmpty(), "至少需要一条分账规则");
        Set<String> names = new HashSet<>();
        for (RuleConfig rule : config.getRules()) {
            require(rule != null && rule.getName() != null && !rule.getName().isBlank(), "规则名字不能为空");
            require(names.add(rule.getName().trim()), "规则名字重复: " + rule.getName());
            require(rule.getSteps() != null && !rule.getSteps().isEmpty(), "规则「" + rule.getName() + "」没有任何步骤");
            validateSteps(config, rule, keys);
        }
    }

    private static void validateSteps(CalculatorConfig config, RuleConfig rule, Set<String> keys) {
        Set<String> claimed = new HashSet<>();
        Set<String> remainders = new HashSet<>();
        for (StepConfig step : rule.getSteps()) {
            require(step != null && step.getType() != null, "规则步骤缺少类型");
            if ("transfer".equals(step.getType())) {
                require(keys.contains(step.getFrom()), "转出人不存在: " + step.getFrom());
                BigDecimal ratio = step.getRatio() == null ? BigDecimal.ONE : step.getRatio();
                require(ratio.signum() >= 0 && ratio.compareTo(BigDecimal.ONE) <= 0, "转出比例必须在 0 到 1 之间");
                require(validateShares(config, step.getTo(), keys, false).signum() > 0, "接收份额合计必须大于 0");
                continue;
            }
            require("share".equals(step.getType()), "不认识的规则步骤: " + step.getType());
            String source = step.getSource();
            require(source != null, "分摊步骤缺少费用来源");
            boolean remainder = source.startsWith("remainder:");
            String origin = remainder ? source.substring("remainder:".length()) : source;
            require("total".equals(origin) || FEES.contains(origin), "不认识的费用来源: " + source);
            if (remainder) {
                require(remainders.remove(origin), "没有可用的剩余费用，或已分摊过: " + origin);
            } else {
                Set<String> sources = "total".equals(origin) ? FEES : Set.of(origin);
                for (String fee : sources) require(claimed.add(fee), "费用重复分摊: " + fee);
            }
            BigDecimal sum = validateShares(config, step.getShares(), keys, true);
            if (Boolean.TRUE.equals(step.getPartial())) {
                require(sum.compareTo(BigDecimal.valueOf(config.getMembers().size())) <= 0, "只取一部分时，份额合计不能超过 1");
                remainders.add(origin);
            } else {
                require(sum.signum() > 0, "完整分摊的份额合计必须大于 0");
            }
        }
        require(remainders.isEmpty(), "规则「" + rule.getName() + "」还有剩余费用，请添加分摊剩余费用的步骤");
        require(claimed.containsAll(FEES), "规则「" + rule.getName() + "」需覆盖总费用，或分别分摊水、电、燃气费");
    }

    private static BigDecimal validateShares(CalculatorConfig config, List<ShareConfig> shares,
                                            Set<String> keys, boolean allowDays) {
        require(shares != null && !shares.isEmpty(), "步骤没有分配对象");
        BigDecimal sum = BigDecimal.ZERO;
        for (ShareConfig share : shares) {
            require(share != null && keys.contains(share.getMember()), "规则引用了不存在的人员");
            String basis = share.getBasis();
            boolean days = usesStayDays(basis);
            require(basis == null || basis.isBlank() || (allowDays && days), "不认识的份额计算方式: " + basis);
            require(!days || Boolean.TRUE.equals(config.getUseStayDays()), "规则按居住天数分摊，请开启居住天数输入");
            BigDecimal weight = share.getWeight();
            if (weight == null && (days || !allowDays)) weight = BigDecimal.ONE;
            require(weight != null && weight.signum() >= 0, "份额必须为非负数");
            // 用总人数作为共同分母，精确比较部分分摊在住满整月时的最大比例。
            if (!STAY_DAYS_PER_MEMBER.equals(basis)) {
                weight = weight.multiply(BigDecimal.valueOf(config.getMembers().size()));
            }
            sum = sum.add(weight);
        }
        return sum;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }
}
