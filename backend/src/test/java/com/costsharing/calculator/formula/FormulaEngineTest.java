package com.costsharing.calculator.formula;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 覆盖业务规则、费用守恒和金额边界。李子房主按入住天数占比和总人数承担费用。
 */
class FormulaEngineTest {

    private static BillInput input(String water, String electric, String gas, Integer totalDays, Integer ownerDays) {
        return new BillInput(new BigDecimal(water), new BigDecimal(electric), new BigDecimal(gas), totalDays, ownerDays);
    }

    private static Map<String, BigDecimal> amounts(CalculatorConfig config, String rule, BillInput input) {
        return FormulaEngine.calculate(config, rule, input).stream()
                .collect(Collectors.toMap(ShareResult::getKey, ShareResult::getAmount));
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    @Test
    void ownerPaysStayDaysProportionDividedByAllMembers() {
        // total = 300，30 天里住 15 天
        // owner = 300 * (15/30) / 3 = 50.00，剩余 250，两人各 125.00
        BillInput input = input("100", "100", "100", 30, 15);
        Map<String, BigDecimal> got = amounts(DefaultConfigs.lizi(), "默认分账", input);

        BigDecimal total = new BigDecimal("300");
        BigDecimal owner = money(total.multiply(new BigDecimal("0.5")).divide(new BigDecimal("3"), 8, RoundingMode.HALF_UP));
        BigDecimal remaining = total.subtract(owner);
        BigDecimal half = money(remaining.divide(new BigDecimal("2"), 8, RoundingMode.HALF_UP));

        assertEquals(owner, got.get("owner"));
        assertEquals(half, got.get("sister"));
        assertEquals(half, got.get("datou"));
    }

    @Test
    void liziOwnerStaysZeroDays() {
        Map<String, BigDecimal> got = amounts(DefaultConfigs.lizi(), "默认分账",
                input("100", "50", "50", 31, 0));
        assertEquals(new BigDecimal("0.00"), got.get("owner"));
        assertEquals(new BigDecimal("100.00"), got.get("sister"));
        assertEquals(new BigDecimal("100.00"), got.get("datou"));
    }

    @Test
    void geziNormalMatchesBackendService() {
        // 水 40 + 燃气 20 = 60，每人 15；电费 60：李子鸽子各 10，春风橙子各 20
        BillInput input = input("40", "60", "20", null, null);
        Map<String, BigDecimal> got = amounts(DefaultConfigs.gezi(), "普通分账", input);

        assertEquals(new BigDecimal("25.00"), got.get("lizi"));
        assertEquals(new BigDecimal("25.00"), got.get("gezi"));
        assertEquals(new BigDecimal("35.00"), got.get("chunfeng"));
        assertEquals(new BigDecimal("35.00"), got.get("chengzi"));
    }

    @Test
    void geziSpecialHalvesChengziAndSplitsOntoLiziAndGezi() {
        BillInput input = input("40", "60", "20", null, null);
        Map<String, BigDecimal> got = amounts(DefaultConfigs.gezi(), "特殊分账", input);

        // 普通分账后橙子 35，转出一半 17.50，李子和鸽子各加 8.75
        assertEquals(new BigDecimal("33.75"), got.get("lizi"));
        assertEquals(new BigDecimal("33.75"), got.get("gezi"));
        assertEquals(new BigDecimal("35.00"), got.get("chunfeng"));
        assertEquals(new BigDecimal("17.50"), got.get("chengzi"));
    }

    @Test
    void rejectsUnknownStepType() {
        CalculatorConfig config = DefaultConfigs.lizi();
        config.getRules().get(0).getSteps().get(0).setType("eval");
        assertThrows(IllegalArgumentException.class,
                () -> FormulaEngine.calculate(config, "默认分账", input("1", "1", "1", 30, 10)));
    }

    @Test
    void ownerShareTracksMemberCountAndOthersUseConfiguredRatios() {
        CalculatorConfig config = DefaultConfigs.lizi();
        config.setMembers(new ArrayList<>(config.getMembers()));
        config.getMembers().add(new MemberConfig("new", "新室友", "👤"));
        config.getRules().get(0).getSteps().get(1).setShares(List.of(
                new ShareConfig("sister", BigDecimal.ONE), new ShareConfig("datou", new BigDecimal("2")),
                new ShareConfig("new", BigDecimal.ONE)));
        Map<String, BigDecimal> got = amounts(config, null, input("320", "0", "0", 30, 15));
        assertEquals(new BigDecimal("40.00"), got.get("owner"));
        assertEquals(new BigDecimal("70.00"), got.get("sister"));
        assertEquals(new BigDecimal("140.00"), got.get("datou"));
        assertEquals(new BigDecimal("70.00"), got.get("new"));
    }

    @Test
    void ownerStaysWholeMonth() {
        Map<String, BigDecimal> got = amounts(DefaultConfigs.lizi(), null, input("300", "0", "0", 30, 30));
        assertEquals(new BigDecimal("100.00"), got.get("owner"));
        assertEquals(new BigDecimal("100.00"), got.get("sister"));
        assertEquals(new BigDecimal("100.00"), got.get("datou"));
    }

    @Test
    void roundingPreservesTotalAndDoesNotAllocateToZeroWeights() {
        CalculatorConfig config = DefaultConfigs.lizi();
        config.getRules().get(0).setSteps(List.of(share("total", false,
                new ShareConfig("owner", BigDecimal.ZERO), new ShareConfig("sister", BigDecimal.ONE),
                new ShareConfig("datou", BigDecimal.ONE))));
        Map<String, BigDecimal> got = amounts(config, null, input("2.01", "0", "0", null, null));
        assertEquals(new BigDecimal("0.00"), got.get("owner"));
        assertEquals(new BigDecimal("1.01"), got.get("sister"));
        assertEquals(new BigDecimal("1.00"), got.get("datou"));
    }

    @Test
    void partialStepKeepsExactHalfCentBeforeRounding() {
        CalculatorConfig config = DefaultConfigs.lizi();
        config.getRules().get(0).getSteps().get(0).getShares().get(0).setBasis(CalculatorConfigValidator.STAY_DAYS);
        Map<String, BigDecimal> got = amounts(config, null, input("6.03", "0", "0", 6, 1));
        assertEquals(new BigDecimal("1.01"), got.get("owner"));
        assertEquals(new BigDecimal("6.03"), got.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    @Test
    void zeroWeightInPartialStepAndTransferStaysZero() {
        CalculatorConfig config = DefaultConfigs.lizi();
        config.getRules().get(0).getSteps().get(0).getShares().get(0).setWeight(BigDecimal.ZERO);
        StepConfig transfer = new StepConfig();
        transfer.setType("transfer");
        transfer.setFrom("sister");
        transfer.setRatio(new BigDecimal("0.5"));
        transfer.setTo(List.of(new ShareConfig("owner", BigDecimal.ZERO), new ShareConfig("datou", BigDecimal.ONE)));
        List<StepConfig> steps = new ArrayList<>(config.getRules().get(0).getSteps());
        steps.add(transfer);
        config.getRules().get(0).setSteps(steps);
        Map<String, BigDecimal> got = amounts(config, null, input("100", "0", "0", 30, 15));
        assertEquals(new BigDecimal("0.00"), got.get("owner"));
        assertEquals(new BigDecimal("25.00"), got.get("sister"));
        assertEquals(new BigDecimal("75.00"), got.get("datou"));
    }

    @Test
    void rejectsDuplicateAndOverlappingSources() {
        CalculatorConfig config = DefaultConfigs.lizi();
        config.getRules().get(0).setSteps(List.of(share("total", false, one()), share("total", false, one())));
        assertThrows(IllegalArgumentException.class, () -> CalculatorConfigValidator.validate(config));
        config.getRules().get(0).setSteps(List.of(share("total", false, one()), share("water", false, one())));
        assertThrows(IllegalArgumentException.class, () -> CalculatorConfigValidator.validate(config));
    }

    @Test
    void rejectsMissingFeesAndUnconsumedOrReusedRemainders() {
        CalculatorConfig config = DefaultConfigs.lizi();
        config.getRules().get(0).setSteps(List.of(share("water", false, one())));
        assertThrows(IllegalArgumentException.class, () -> CalculatorConfigValidator.validate(config));
        CalculatorConfig incomplete = DefaultConfigs.lizi();
        incomplete.getRules().get(0).setSteps(List.of(incomplete.getRules().get(0).getSteps().get(0)));
        assertThrows(IllegalArgumentException.class, () -> CalculatorConfigValidator.validate(incomplete));
        CalculatorConfig reused = DefaultConfigs.lizi();
        List<StepConfig> steps = new ArrayList<>(reused.getRules().get(0).getSteps());
        steps.add(share("remainder:total", false, one()));
        reused.getRules().get(0).setSteps(steps);
        assertThrows(IllegalArgumentException.class, () -> CalculatorConfigValidator.validate(reused));
    }

    @Test
    void supportsSuccessivePartialStepsOnRemainder() {
        CalculatorConfig config = DefaultConfigs.lizi();
        config.getRules().get(0).setSteps(List.of(
                share("total", true, new ShareConfig("owner", new BigDecimal("0.5"))),
                share("remainder:total", true, new ShareConfig("sister", new BigDecimal("0.5"))),
                share("remainder:total", false, new ShareConfig("datou", BigDecimal.ONE))));
        Map<String, BigDecimal> got = amounts(config, null, input("100", "0", "0", null, null));
        assertEquals(new BigDecimal("50.00"), got.get("owner"));
        assertEquals(new BigDecimal("25.00"), got.get("sister"));
        assertEquals(new BigDecimal("25.00"), got.get("datou"));
    }

    @Test
    void rejectsDisabledStayDaysAndInvalidWeightsOrRatios() {
        CalculatorConfig config = DefaultConfigs.lizi();
        config.setUseStayDays(false);
        assertThrows(IllegalArgumentException.class, () -> CalculatorConfigValidator.validate(config));
        config.setUseStayDays(true);
        config.getRules().get(0).getSteps().get(0).getShares().get(0).setWeight(new BigDecimal("-1"));
        assertThrows(IllegalArgumentException.class, () -> CalculatorConfigValidator.validate(config));
        CalculatorConfig zero = DefaultConfigs.lizi();
        zero.getRules().get(0).setSteps(List.of(share("total", false, new ShareConfig("owner", BigDecimal.ZERO))));
        assertThrows(IllegalArgumentException.class, () -> CalculatorConfigValidator.validate(zero));
        CalculatorConfig gezi = DefaultConfigs.gezi();
        gezi.getRules().get(1).getSteps().get(3).setRatio(new BigDecimal("1.1"));
        assertThrows(IllegalArgumentException.class, () -> CalculatorConfigValidator.validate(gezi));
    }

    @Test
    void randomAmountsAndTinyFeesRemainNonnegativeAndSumExactly() {
        Random random = new Random(1);
        for (int i = 0; i < 400; i++) {
            BigDecimal total = BigDecimal.valueOf(i < 10 ? i : random.nextInt(100000), 2);
            for (CalculatorConfig config : List.of(DefaultConfigs.lizi(), DefaultConfigs.gezi())) {
                for (RuleConfig rule : config.getRules()) {
                    List<ShareResult> result = FormulaEngine.calculate(config, rule.getName(),
                            input(total.toPlainString(), "0", "0", 31, random.nextInt(32)));
                    assertEquals(total, result.stream().map(ShareResult::getAmount).reduce(new BigDecimal("0.00"), BigDecimal::add));
                    result.forEach(share -> org.junit.jupiter.api.Assertions.assertTrue(share.getAmount().signum() >= 0));
                }
            }
        }
    }

    @Test
    void tinyFeesAcrossManyPeopleDoNotCreateNegativeTailAmounts() {
        CalculatorConfig config = DefaultConfigs.lizi();
        config.setMembers(IntStream.range(0, 8)
                .mapToObj(i -> new MemberConfig("m" + i, "人员" + i, "👤")).toList());
        config.getRules().get(0).setSteps(List.of(share("total", false, config.getMembers().stream()
                .map(member -> new ShareConfig(member.getKey(), BigDecimal.ONE)).toArray(ShareConfig[]::new))));
        var got = amounts(config, null, input("0.04", "0", "0", null, null));
        assertEquals(new BigDecimal("0.04"), got.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));
        got.values().forEach(value -> org.junit.jupiter.api.Assertions.assertTrue(value.signum() >= 0));
    }

    private static ShareConfig one() {
        return new ShareConfig("owner", BigDecimal.ONE);
    }

    private static StepConfig share(String source, boolean partial, ShareConfig... shares) {
        StepConfig step = new StepConfig();
        step.setType("share");
        step.setSource(source);
        step.setPartial(partial);
        step.setShares(List.of(shares));
        return step;
    }
}
