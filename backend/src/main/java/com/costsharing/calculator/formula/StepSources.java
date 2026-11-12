package com.costsharing.calculator.formula;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/** 单选旧规则和多选费用组共用同一种费用流向。 */
public final class StepSources {
    private StepSources() {}

    public static List<String> of(StepConfig step) {
        List<String> sources = step.getSources() != null ? step.getSources()
                : step.getSource() == null ? List.of() : List.of(step.getSource());
        if (sources.isEmpty()) throw new IllegalArgumentException("请至少选择一项要分配的费用");
        if (sources.stream().anyMatch(source -> source == null || source.isBlank())) {
            throw new IllegalArgumentException("费用来源不能为空");
        }
        if (new HashSet<>(sources).size() != sources.size()) throw new IllegalArgumentException("同一步不能重复选择费用");
        return sources;
    }

    public static String origin(StepConfig step) {
        List<String> sources = of(step);
        if (sources.size() == 1) return sources.get(0).replaceFirst("^remainder:", "");
        // 长度前缀保证组合及嵌套组合不碰撞；排序使选择顺序不影响剩余费用关联。
        return "group:" + sources.stream().sorted().map(source -> source.length() + ":" + source).collect(Collectors.joining());
    }
}
