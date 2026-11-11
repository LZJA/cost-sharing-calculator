package com.costsharing.calculator.formula;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberConfig {

    /** 稳定标识，改名字不会变，规则步骤用它引用人员。 */
    private String key;

    private String name;

    private String emoji;
}
