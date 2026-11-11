package com.costsharing.calculator.formula;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FeeCatalog {
    private FeeCatalog() {}

    public static List<FeeConfig> legacyFees() {
        return List.of(new FeeConfig("water", "水费", "💧"), new FeeConfig("electric", "电费", "⚡"),
                new FeeConfig("gas", "燃气费", "🔥"));
    }

    /** 仅缺少 fees 字段的旧配置回退到三项费用，明确传空列表应由校验拒绝。 */
    public static List<FeeConfig> definitions(CalculatorConfig config) {
        return config.getFees() == null ? legacyFees() : config.getFees();
    }

    public static Map<String, BigDecimal> amounts(CalculatorConfig config, BillInput input) {
        Map<String, BigDecimal> supplied = input.getFees();
        if (supplied == null) {
            if (input.getWaterBill() == null && input.getElectricBill() == null && input.getGasBill() == null) {
                throw new IllegalArgumentException("请填写费用明细");
            }
            supplied = new LinkedHashMap<>();
            supplied.put("water", input.getWaterBill());
            supplied.put("electric", input.getElectricBill());
            supplied.put("gas", input.getGasBill());
            for (FeeConfig fee : definitions(config)) {
                if (!supplied.containsKey(fee.getKey())) throw new IllegalArgumentException("请填写费用明细");
            }
            // 旧输入允许未使用的零金额列，不能丢弃未配置的非零费用。
            supplied.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue().signum() == 0);
        }
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (FeeConfig fee : definitions(config)) result.put(fee.getKey(), money(supplied.get(fee.getKey())));
        for (String key : supplied.keySet()) {
            if (!result.containsKey(key)) throw new IllegalArgumentException("费用项不存在或已删除，请刷新计算器: " + key);
        }
        return result;
    }

    public static List<FeeAmount> snapshot(CalculatorConfig config, Map<String, BigDecimal> amounts) {
        return definitions(config).stream().map(fee ->
                new FeeAmount(fee.getKey(), fee.getName().trim(), fee.getEmoji(), amounts.get(fee.getKey()))).toList();
    }

    private static BigDecimal money(BigDecimal value) {
        if (value == null) return new BigDecimal("0.00");
        if (value.signum() < 0) throw new IllegalArgumentException("费用不能为负数");
        try {
            return value.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("费用最多保留两位小数");
        }
    }
}
