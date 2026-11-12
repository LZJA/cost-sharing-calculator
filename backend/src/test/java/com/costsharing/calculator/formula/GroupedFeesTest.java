package com.costsharing.calculator.formula;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class GroupedFeesTest {
    private CalculatorConfig config(boolean parking) {
        var config = DefaultConfigs.lizi();
        if (parking) {
            var fees = new ArrayList<>(config.getFees());
            fees.add(new FeeConfig("parking", "车位费", "🚗"));
            config.setFees(fees);
        }
        return config;
    }

    private StepConfig step(boolean partial, String... sources) {
        var step = new StepConfig();
        step.setType("share");
        step.setSources(List.of(sources));
        step.setPartial(partial);
        step.setShares(List.of(new ShareConfig("owner", BigDecimal.ONE), new ShareConfig("sister", BigDecimal.ONE)));
        return step;
    }

    private void assign(StepConfig step, String member, String weight) {
        step.setShares(List.of(new ShareConfig(member, new BigDecimal(weight))));
    }

    @Test
    void combinesBeforeRoundingInsteadOfRepeatingEachFeeSplit() {
        var config = config(false);
        var water = step(false, "water");
        assign(water, "datou", "1");
        var combined = step(false, "electric", "gas");
        config.getRules().get(0).setSteps(List.of(water, combined));
        var got = FormulaEngine.calculate(config, null, new BillInput(Map.of("water", new BigDecimal("10"),
                "electric", new BigDecimal("20.01"), "gas", new BigDecimal("30.01")), null, null));
        assertEquals(new BigDecimal("25.01"), got.get(0).getAmount());
        assertEquals(new BigDecimal("25.01"), got.get(1).getAmount());
        assertEquals(new BigDecimal("10.00"), got.get(2).getAmount());
    }

    @Test
    void electricAndParkingCanBeGroupedWhileWaterAndGasUseOtherSteps() {
        var config = config(true);
        var water = step(false, "water");
        assign(water, "datou", "1");
        var combined = step(false, "electric", "parking");
        combined.getShares().get(1).setWeight(new BigDecimal("2"));
        var gas = step(false, "gas");
        assign(gas, "datou", "1");
        config.getRules().get(0).setSteps(List.of(water, combined, gas));
        var got = FormulaEngine.calculate(config, null, input());
        assertEquals(new BigDecimal("20.00"), got.get(0).getAmount());
        assertEquals(new BigDecimal("40.00"), got.get(1).getAmount());
        assertEquals(new BigDecimal("40.00"), got.get(2).getAmount());
    }

    @Test
    void groupedPartialSplitHasAConsumableRemainderWithStableIdentity() {
        var config = config(false);
        var water = step(false, "water");
        assign(water, "datou", "1");
        var combined = step(true, "electric", "gas");
        assign(combined, "owner", "0.5");
        assertEquals("group:8:electric3:gas", StepSources.origin(combined));
        assertEquals(StepSources.origin(combined), StepSources.origin(step(true, "gas", "electric")));
        var rest = step(false, "remainder:" + StepSources.origin(combined));
        assign(rest, "sister", "1");
        config.getRules().get(0).setSteps(List.of(water, combined, rest));
        var got = FormulaEngine.calculate(config, null, new BillInput(Map.of("water", new BigDecimal("10"),
                "electric", new BigDecimal("20"), "gas", new BigDecimal("30")), null, null));
        assertEquals(new BigDecimal("25.00"), got.get(0).getAmount());
        assertEquals(new BigDecimal("25.00"), got.get(1).getAmount());
    }

    @Test
    void remainderAndNewFeesCanBeCombinedAndPartiallySplitAgain() {
        var config = config(true);
        var water = step(true, "water");
        assign(water, "owner", "0.5");
        var combined = step(true, "remainder:water", "electric", "parking");
        assign(combined, "sister", "0.5");
        var rest = step(false, "remainder:" + StepSources.origin(combined), "gas");
        assign(rest, "datou", "1");
        config.getRules().get(0).setSteps(List.of(water, combined, rest));
        var got = FormulaEngine.calculate(config, null, input());
        assertEquals(new BigDecimal("5.00"), got.get(0).getAmount());
        assertEquals(new BigDecimal("32.50"), got.get(1).getAmount());
        assertEquals(new BigDecimal("62.50"), got.get(2).getAmount());
    }

    @Test
    void rejectsDuplicateDeletedEmptyAndOverlappingSources() {
        var config = config(false);
        for (var sources : List.of(List.of("water", "water"), List.of("total", "water"), List.of("deleted"), List.<String>of())) {
            var invalid = step(false, "total");
            invalid.setSources(sources);
            invalid.setSource("total"); // 空的 sources 不能回退到旧字段。
            config.getRules().get(0).setSteps(List.of(invalid));
            assertThrows(IllegalArgumentException.class, () -> CalculatorConfigValidator.validate(config));
        }
        var group = step(true, "water", "electric", "gas");
        assign(group, "owner", "0.5");
        var rest = step(false, "remainder:" + StepSources.origin(group));
        config.getRules().get(0).setSteps(List.of(group, rest, rest));
        assertThrows(IllegalArgumentException.class, () -> CalculatorConfigValidator.validate(config));
    }

    private BillInput input() {
        return new BillInput(Map.of("water", new BigDecimal("10"), "electric", new BigDecimal("20"),
                "gas", new BigDecimal("30"), "parking", new BigDecimal("40")), null, null);
    }
}
