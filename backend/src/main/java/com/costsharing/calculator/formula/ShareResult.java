package com.costsharing.calculator.formula;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** 落库时把当时的名字和头像一起记下，之后改名不影响旧账单。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShareResult {

    private String key;
    private String name;
    private String emoji;
    private BigDecimal amount;
}
