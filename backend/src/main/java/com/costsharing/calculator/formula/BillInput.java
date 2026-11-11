package com.costsharing.calculator.formula;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BillInput {

    private BigDecimal waterBill;
    private BigDecimal electricBill;
    private BigDecimal gasBill;
    private Integer totalDays;
    private Integer ownerDays;
    private Map<String, BigDecimal> fees;

    public BillInput(BigDecimal water, BigDecimal electric, BigDecimal gas, Integer totalDays, Integer ownerDays) {
        this(water, electric, gas, totalDays, ownerDays, null);
    }

    public BillInput(Map<String, BigDecimal> fees, Integer totalDays, Integer ownerDays) {
        this(null, null, null, totalDays, ownerDays, fees);
    }
}
