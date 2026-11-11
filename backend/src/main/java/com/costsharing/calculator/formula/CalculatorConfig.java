package com.costsharing.calculator.formula;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 一个计算器的全部配置：人员、是否按居住天数、分账规则。
 */
@Data
@NoArgsConstructor
public class CalculatorConfig {

    private List<MemberConfig> members = new ArrayList<>();

    /**
     * 为 true 时计算页需要填写总天数和居住天数。
     */
    private Boolean useStayDays = false;

    private List<RuleConfig> rules = new ArrayList<>();
}
