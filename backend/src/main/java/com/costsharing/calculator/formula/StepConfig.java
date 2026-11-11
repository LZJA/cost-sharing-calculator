package com.costsharing.calculator.formula;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 一个计算步骤。
 * share：把 source（water/electric/gas/total）按 shares 的份额分给对应的人。
 * transfer：把 from 当前金额的 ratio 按 to 的份额转给其他人。
 */
@Data
@NoArgsConstructor
public class StepConfig {

    private String type;

    private String source;

    /**
     * share 专用。为 true 时，这一步只按各份额的绝对比例取费用的一部分
     * （份额合计可以小于 1），剩下的不分配，留给后面的步骤。
     * 李子的房主就是这样：先拿走 total 的「居住天数占比 / 2」，其余两人再平分剩下的。
     */
    private Boolean partial;

    private List<ShareConfig> shares;

    private String from;

    private BigDecimal ratio;

    private List<ShareConfig> to;
}
