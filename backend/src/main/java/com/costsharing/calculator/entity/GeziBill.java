package com.costsharing.calculator.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 鸽子的账单记录实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "gezi_bills")
public class GeziBill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 水费
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal waterBill = BigDecimal.ZERO;

    /**
     * 电费
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal electricBill = BigDecimal.ZERO;

    /**
     * 燃气费
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal gasBill = BigDecimal.ZERO;

    /**
     * 分账规则: "普通分账" 或 "特殊分账"
     */
    @Column(nullable = false, length = 20)
    private String splitRule;

    /**
     * 总费用
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    /**
     * 李子应承担费用
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal liziAmount;

    /**
     * 鸽子应承担费用
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal geziAmount;

    /**
     * 春风应承担费用
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal chunfengAmount;

    /**
     * 橙子应承担费用
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal chengziAmount;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
