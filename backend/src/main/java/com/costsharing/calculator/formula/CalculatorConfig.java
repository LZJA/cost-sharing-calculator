package com.costsharing.calculator.formula;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 一个计算器的全部配置：人员、是否按居住天数、分账规则。
 */
@Data
@NoArgsConstructor
public class CalculatorConfig {

    /** null 兼容旧配置；新计算器明确提供自定义费用项。 */
    private List<FeeConfig> fees;

    private List<MemberConfig> members = new ArrayList<>();

    /**
     * 为 true 时计算页显示周期总天数和参与天数；字段名保留以兼容旧配置。
     */
    private Boolean useStayDays = false;

    /** 通用天数输入的显示名称，旧配置可省略。 */
    private Map<String, String> dayLabels;

    private List<RuleConfig> rules = new ArrayList<>();
}
