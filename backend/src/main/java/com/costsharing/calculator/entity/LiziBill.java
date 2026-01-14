package com.costsharing.calculator.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 李子的账单记录实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "lizi_bills", indexes = {
    @Index(name = "idx_month_year", columnList = "month,year")
})
public class LiziBill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 月份 (1-12)
     */
    @Column(nullable = false)
    private Integer month;

    /**
     * 年份
     */
    @Column(nullable = false)
    private Integer year;

    /**
     * 月份总天数
     */
    @Column(nullable = false)
    private Integer totalDays;

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
     * 房主居住天数
     */
    @Column(nullable = false)
    private Integer ownerDays = 0;

    /**
     * 总费用
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    /**
     * 房主应承担费用
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal ownerAmount;

    /**
     * 剩余费用
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal remainingAmount;

    /**
     * 谢林珠应承担费用
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal sisterAmount;

    /**
     * 张锦豪应承担费用
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal datouAmount;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
