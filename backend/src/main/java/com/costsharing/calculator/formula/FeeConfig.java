package com.costsharing.calculator.formula;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 费用名称可修改，key 保持稳定，供规则和历史统计关联。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeeConfig {
    private String key;
    private String name;
    private String emoji;
}
