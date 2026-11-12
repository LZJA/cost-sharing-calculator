package com.costsharing.calculator.formula;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 一个计算步骤。
 * share：把 source（费用标识、total 或 remainder:费用标识）按 shares 的份额分给对应的人。
 * transfer：把 from 当前金额的 ratio 按 to 的份额转给其他人。
 */
@Data
@NoArgsConstructor
public class StepConfig {

    private String type;

    /** 编辑器展示方式；金额仍由 partial、basis 和 weight 决定，兼容旧配置省略。 */
    private String allocationMode;

    private String source;

    /** 多选费用，先合计金额再分摊。非 null 时优先于旧 source，空列表无效。 */
    private List<String> sources;

    /**
     * share 专用。为 true 时，这一步只按各份额的绝对比例取费用的一部分
     * （份额合计可以小于 1），剩下的不分配，留给后面的步骤。
     * 例如先按「参与天数占比 / 总人数」提取部分费用，再分摊剩余费用。
     */
    private Boolean partial;

    private List<ShareConfig> shares;

    private String from;

    private BigDecimal ratio;

    private List<ShareConfig> to;
}
