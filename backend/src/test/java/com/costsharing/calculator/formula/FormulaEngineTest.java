package com.costsharing.calculator.formula;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 用同一组金额，对照当前 LiziBillService、GeziBillService 的算法。
 * 李子的除数以后端为准（除以 2），不是前端 config.js 里的 3。
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
    void liziMatchesBackendService() {
        // total = 300，30 天里住 15 天
        // owner = 300 * (15/30) / 2 = 75.00
        // 剩余 225，两人各 112.50
        BillInput input = input("100", "100", "100", 30, 15);
        Map<String, BigDecimal> got = amounts(DefaultConfigs.lizi(), "默认分账", input);

        BigDecimal total = new BigDecimal("300");
        BigDecimal owner = money(total.multiply(new BigDecimal("0.5")).divide(new BigDecimal("2"), 8, RoundingMode.HALF_UP));
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
}
