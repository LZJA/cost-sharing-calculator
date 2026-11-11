package com.costsharing.calculator.formula;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/** 账单中的费用快照，计算器改名、删除费用项都不会改写旧账。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeeAmount {
    private String key;
    private String name;
    private String emoji;
    private BigDecimal amount;
}
