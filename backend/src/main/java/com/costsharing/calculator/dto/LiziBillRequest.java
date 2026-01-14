package com.costsharing.calculator.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 李子账单请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LiziBillRequest {

    @NotNull(message = "月份不能为空")
    @Min(value = 1, message = "月份必须在1-12之间")
    @Max(value = 12, message = "月份必须在1-12之间")
    private Integer month;

    @NotNull(message = "年份不能为空")
    @Min(value = 2020, message = "年份不能小于2020")
    private Integer year;

    @NotNull(message = "总天数不能为空")
    @Min(value = 1, message = "总天数必须在1-31之间")
    @Max(value = 31, message = "总天数必须在1-31之间")
    private Integer totalDays;

    @NotNull(message = "水费不能为空")
    @DecimalMin(value = "0.0", inclusive = true, message = "水费不能为负数")
    private BigDecimal waterBill;

    @NotNull(message = "电费不能为空")
    @DecimalMin(value = "0.0", inclusive = true, message = "电费不能为负数")
    private BigDecimal electricBill;

    @NotNull(message = "燃气费不能为空")
    @DecimalMin(value = "0.0", inclusive = true, message = "燃气费不能为负数")
    private BigDecimal gasBill;

    @NotNull(message = "房主居住天数不能为空")
    @Min(value = 0, message = "房主居住天数不能为负数")
    private Integer ownerDays;
}
