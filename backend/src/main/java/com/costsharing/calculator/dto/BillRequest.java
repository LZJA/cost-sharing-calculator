package com.costsharing.calculator.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 保存账单只收原始输入。每人金额由服务端按规则重算，不采用前端传来的结果。
 */
@Data
@NoArgsConstructor
public class BillRequest {

    @NotNull(message = "计算器不能为空")
    private Long calculatorId;

    @NotNull(message = "年份不能为空")
    @Min(value = 2020, message = "年份不能小于2020")
    private Integer year;

    @NotNull(message = "月份不能为空")
    @Min(value = 1, message = "月份必须在1-12之间")
    @Max(value = 12, message = "月份必须在1-12之间")
    private Integer month;

    /** 新客户端传费用标识与金额；旧三列仅兼容旧客户端。 */
    private Map<String, BigDecimal> fees;

    @DecimalMin(value = "0.0", message = "水费不能为负数")
    private BigDecimal waterBill;

    @DecimalMin(value = "0.0", message = "电费不能为负数")
    private BigDecimal electricBill;

    @DecimalMin(value = "0.0", message = "燃气费不能为负数")
    private BigDecimal gasBill;

    @Min(value = 1, message = "周期总天数必须大于0")
    private Integer totalDays;

    @Min(value = 0, message = "参与天数不能为负数")
    /** 兼容旧客户端，新客户端使用 days。 */
    private Integer ownerDays;

    @Min(value = 0, message = "参与天数不能为负数")
    private Integer days;

    private String ruleName;
}
