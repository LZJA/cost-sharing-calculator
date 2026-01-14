package com.costsharing.calculator.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 鸽子账单请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeziBillRequest {

    @NotNull(message = "水费不能为空")
    @DecimalMin(value = "0.0", inclusive = true, message = "水费不能为负数")
    private BigDecimal waterBill;

    @NotNull(message = "电费不能为空")
    @DecimalMin(value = "0.0", inclusive = true, message = "电费不能为负数")
    private BigDecimal electricBill;

    @NotNull(message = "燃气费不能为空")
    @DecimalMin(value = "0.0", inclusive = true, message = "燃气费不能为负数")
    private BigDecimal gasBill;

    @NotBlank(message = "分账规则不能为空")
    private String splitRule;
}
