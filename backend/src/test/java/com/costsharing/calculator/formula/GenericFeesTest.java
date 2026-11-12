package com.costsharing.calculator.formula;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class GenericFeesTest {
    @Test
    void fixedPercentagesAtOneHundredNeedNoZeroRemainderStep() {
        var config = DefaultConfigs.lizi();
        var step = share("total", new ShareConfig("owner", new BigDecimal("0.5")),
                new ShareConfig("sister", new BigDecimal("0.5")));
        step.setPartial(true);
        step.setAllocationMode("percent");
        config.getRules().get(0).setSteps(List.of(step));
        var result = FormulaEngine.calculate(config, null, new BillInput(
                new BigDecimal("100"), BigDecimal.ZERO, BigDecimal.ZERO, null, null));
        assertEquals(new BigDecimal("50.00"), result.get(0).getAmount());
        assertEquals(new BigDecimal("50.00"), result.get(1).getAmount());
        step.getShares().get(0).setWeight(new BigDecimal("0.25"));
        assertThrows(IllegalArgumentException.class, () -> CalculatorConfigValidator.validate(config));
    }

    @Test
    void editorModeNeverChangesTheActualStoredSplitWeights() {
        var config = DefaultConfigs.lizi();
        var step = share("total", new ShareConfig("owner", BigDecimal.ONE),
                new ShareConfig("sister", new BigDecimal("2")));
        step.setAllocationMode("equal");
        config.getRules().get(0).setSteps(List.of(step));
        var result = FormulaEngine.calculate(config, null, new BillInput(
                new BigDecimal("90"), BigDecimal.ZERO, BigDecimal.ZERO, null, null));
        assertEquals(new BigDecimal("30.00"), result.get(0).getAmount());
        assertEquals(new BigDecimal("60.00"), result.get(1).getAmount());
    }
    private CalculatorConfig generic() {
        CalculatorConfig config = DefaultConfigs.lizi();
        config.setFees(List.of(new FeeConfig("rent", "房租", "🏠"), new FeeConfig("net", "网费", "🌐")));
        config.setDayLabels(Map.of("total", "服务周期天数", "counted", "使用天数"));
        return config;
    }

    @Test
    void customFeesUseTotalAndDayRatioWithoutLegacyColumns() {
        var result = FormulaEngine.calculate(generic(), null,
                new BillInput(Map.of("rent", new BigDecimal("300"), "net", new BigDecimal("60")), 90, 45));
        assertEquals(new BigDecimal("60.00"), result.get(0).getAmount());
        assertEquals(new BigDecimal("150.00"), result.get(1).getAmount());
        assertEquals(new BigDecimal("150.00"), result.get(2).getAmount());
    }

    @Test
    void eachCustomFeeCanHaveItsOwnSplitRule() {
        var config = generic();
        config.getRules().get(0).setSteps(List.of(
                share("rent", new ShareConfig("owner", BigDecimal.ONE), new ShareConfig("sister", new BigDecimal("2"))),
                share("net", new ShareConfig("datou", BigDecimal.ONE))));
        var result = FormulaEngine.calculate(config, null,
                new BillInput(Map.of("rent", new BigDecimal("9.99"), "net", new BigDecimal("2.01")), null, null));
        assertEquals(new BigDecimal("3.33"), result.get(0).getAmount());
        assertEquals(new BigDecimal("6.66"), result.get(1).getAmount());
        assertEquals(new BigDecimal("2.01"), result.get(2).getAmount());
    }

    @Test
    void legacyConfigAndLegacyInputKeepWorking() {
        var config = DefaultConfigs.gezi();
        config.setFees(null);
        var result = FormulaEngine.calculate(config, "普通分账", new BillInput(
                new BigDecimal("40"), new BigDecimal("60"), new BigDecimal("20"), null, null));
        assertEquals(new BigDecimal("25.00"), result.get(0).getAmount());
        assertEquals(new BigDecimal("35.00"), result.get(3).getAmount());
    }

    @Test
    void feeSnapshotsPreserveNameAndAmountAfterConfigurationChanges() {
        var config = generic();
        var amounts = FeeCatalog.amounts(config, new BillInput(Map.of("rent", new BigDecimal("100")), 30, 0));
        var snapshot = FeeCatalog.snapshot(config, amounts);
        config.getFees().get(0).setName("新名称");
        assertEquals("房租", snapshot.get(0).getName());
        assertEquals(new BigDecimal("100.00"), snapshot.get(0).getAmount());
        assertEquals(new BigDecimal("0.00"), snapshot.get(1).getAmount());
    }

    @Test
    void rejectsEmptyOrDuplicateFeeDefinitionsAndMissingDayLabels() {
        var config = generic();
        config.setFees(List.of());
        assertThrows(IllegalArgumentException.class, () -> CalculatorConfigValidator.validate(config));
        config.setFees(List.of(new FeeConfig("a", "房租", ""), new FeeConfig("a", "网费", "")));
        assertThrows(IllegalArgumentException.class, () -> CalculatorConfigValidator.validate(config));
        config.setFees(List.of(new FeeConfig("a", "房租", ""), new FeeConfig("b", "房租", "")));
        assertThrows(IllegalArgumentException.class, () -> CalculatorConfigValidator.validate(config));
        config.setFees(List.of(new FeeConfig("total", "房租", "")));
        assertThrows(IllegalArgumentException.class, () -> CalculatorConfigValidator.validate(config));
        config.setFees(List.of(new FeeConfig("a", "房租", "")));
        config.setDayLabels(Map.of("total", "", "counted", "使用天数"));
        assertThrows(IllegalArgumentException.class, () -> CalculatorConfigValidator.validate(config));
    }

    @Test
    void rejectsUnknownMissingOrDuplicateFeeSources() {
        var config = generic();
        config.getRules().get(0).setSteps(List.of(share("rent", new ShareConfig("owner", BigDecimal.ONE))));
        assertThrows(IllegalArgumentException.class, () -> CalculatorConfigValidator.validate(config));
        config.getRules().get(0).setSteps(List.of(share("total", new ShareConfig("owner", BigDecimal.ONE)),
                share("rent", new ShareConfig("owner", BigDecimal.ONE))));
        assertThrows(IllegalArgumentException.class, () -> CalculatorConfigValidator.validate(config));
        config.getRules().get(0).setSteps(List.of(share("deleted", new ShareConfig("owner", BigDecimal.ONE))));
        assertThrows(IllegalArgumentException.class, () -> CalculatorConfigValidator.validate(config));
    }

    @Test
    void rejectsStaleKeysInvalidAmountsAndLegacyRequestsForCustomFees() {
        var config = generic();
        for (var fees : List.of(Map.of("deleted", BigDecimal.ZERO), Map.of("rent", new BigDecimal("-1")),
                Map.of("rent", new BigDecimal("1.001")))) {
            assertThrows(IllegalArgumentException.class, () -> FormulaEngine.calculate(config, null, new BillInput(fees, 30, 0)));
        }
        assertThrows(IllegalArgumentException.class, () -> FormulaEngine.calculate(config, null,
                new BillInput(BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO, 30, 0)));
    }

    private StepConfig share(String source, ShareConfig... shares) {
        StepConfig step = new StepConfig();
        step.setType("share");
        step.setSource(source);
        step.setShares(List.of(shares));
        return step;
    }
}
