package com.costsharing.calculator.formula;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BillInput {

    private BigDecimal waterBill;
    private BigDecimal electricBill;
    private BigDecimal gasBill;
    private Integer totalDays;
    private Integer ownerDays;
}
