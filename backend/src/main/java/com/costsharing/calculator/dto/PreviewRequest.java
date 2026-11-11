package com.costsharing.calculator.dto;

import com.costsharing.calculator.formula.CalculatorConfig;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/** 配规则时的试算，不落库。 */
@Data
@NoArgsConstructor
public class PreviewRequest {

    @NotNull(message = "规则配置不能为空")
    private CalculatorConfig config;

    private String ruleName;

    private Map<String, BigDecimal> fees;

    private BigDecimal waterBill;

    private BigDecimal electricBill;

    private BigDecimal gasBill;

    private Integer totalDays;

    private Integer ownerDays;

    private Integer days;
}
