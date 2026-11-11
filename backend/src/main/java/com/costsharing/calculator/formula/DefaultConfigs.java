package com.costsharing.calculator.formula;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 迁移旧数据时用的默认规则。历史账单金额不重算。
 * 李子：房主承担「总费用 × 居住天数占比 / 总人数」，剩余费用由其他人按配置比例分摊。
 * 鸽子：水费和燃气费四人均摊；电费李子、鸽子各 1/6，春风、橙子各 1/3。
 * 特殊分账在普通分账之后，把橙子当前金额的一半对半转给李子和鸽子。
 */
public final class DefaultConfigs {

    private DefaultConfigs() {
    }

    public static CalculatorConfig lizi() {
        CalculatorConfig config = base(true, List.of(
                member("owner", "房主", "👤"),
                member("sister", "谢林珠", "🌸"),
                member("datou", "张锦豪", "🌻")
        ));

        StepConfig ownerStep = share("total", true, List.of(
                daysShare("owner", BigDecimal.ONE)
        ));
        StepConfig restStep = share("remainder:total", false, List.of(
                fixed("sister", 1),
                fixed("datou", 1)
        ));

        RuleConfig rule = new RuleConfig();
        rule.setName("默认分账");
        rule.setSteps(List.of(ownerStep, restStep));
        config.setRules(List.of(rule));
        return config;
    }

    public static CalculatorConfig gezi() {
        CalculatorConfig config = base(false, List.of(
                member("lizi", "李子", "🍐"),
                member("gezi", "鸽子", "🕊️"),
                member("chunfeng", "春风", "🌸"),
                member("chengzi", "橙子", "🍊")
        ));
        config.setRules(List.of(geziNormal(), geziSpecial()));
        return config;
    }

    private static RuleConfig geziNormal() {
        RuleConfig rule = new RuleConfig();
        rule.setName("普通分账");
        rule.setSteps(new ArrayList<>(List.of(
                share("water", false, quarters()),
                share("gas", false, quarters()),
                share("electric", false, List.of(
                        fixed("lizi", 1),
                        fixed("gezi", 1),
                        fixed("chunfeng", 2),
                        fixed("chengzi", 2)
                ))
        )));
        return rule;
    }

    private static RuleConfig geziSpecial() {
        RuleConfig rule = geziNormal();
        rule.setName("特殊分账");
        StepConfig transfer = new StepConfig();
        transfer.setType("transfer");
        transfer.setFrom("chengzi");
        transfer.setRatio(new BigDecimal("0.5"));
        transfer.setTo(List.of(fixed("lizi", 1), fixed("gezi", 1)));
        rule.getSteps().add(transfer);
        return rule;
    }

    private static CalculatorConfig base(boolean useStayDays, List<MemberConfig> members) {
        CalculatorConfig config = new CalculatorConfig();
        config.setUseStayDays(useStayDays);
        config.setMembers(members);
        return config;
    }

    private static List<ShareConfig> quarters() {
        return List.of(fixed("lizi", 1), fixed("gezi", 1), fixed("chunfeng", 1), fixed("chengzi", 1));
    }

    private static MemberConfig member(String key, String name, String emoji) {
        return new MemberConfig(key, name, emoji);
    }

    private static ShareConfig fixed(String member, int weight) {
        return new ShareConfig(member, new BigDecimal(weight));
    }

    private static ShareConfig daysShare(String member, BigDecimal weight) {
        ShareConfig share = new ShareConfig();
        share.setMember(member);
        share.setWeight(weight);
        share.setBasis(CalculatorConfigValidator.STAY_DAYS_PER_MEMBER);
        return share;
    }

    /** 修复上一版本迁移生成的房主 /2 规则，保留人员资料及其余人员的分摊比例。 */
    public static boolean upgradeLegacyLiziOwnerRule(CalculatorConfig config) {
        if (config == null || config.getRules() == null) return false;
        boolean changed = false;
        for (RuleConfig rule : config.getRules()) {
            if (rule == null || rule.getSteps() == null) continue;
            for (StepConfig step : rule.getSteps()) {
                if (step == null || !"share".equals(step.getType()) || !"total".equals(step.getSource())
                        || !Boolean.TRUE.equals(step.getPartial()) || step.getShares() == null || step.getShares().size() != 1) continue;
                ShareConfig share = step.getShares().get(0);
                if (share != null && "owner".equals(share.getMember())
                        && CalculatorConfigValidator.STAY_DAYS.equals(share.getBasis())
                        && share.getWeight() != null && share.getWeight().compareTo(new BigDecimal("0.5")) == 0) {
                    share.setBasis(CalculatorConfigValidator.STAY_DAYS_PER_MEMBER);
                    share.setWeight(BigDecimal.ONE);
                    changed = true;
                }
            }
        }
        return changed;
    }

    private static StepConfig share(String source, boolean partial, List<ShareConfig> shares) {
        StepConfig step = new StepConfig();
        step.setType("share");
        step.setSource(source);
        step.setPartial(partial);
        step.setShares(shares);
        return step;
    }
}
