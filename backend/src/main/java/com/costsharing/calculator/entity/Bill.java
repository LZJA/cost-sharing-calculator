package com.costsharing.calculator.entity;

import com.costsharing.calculator.formula.ShareResult;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 通用账单。每人金额记在 shares 里，并带上当时的名字，改名不影响旧账。
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "bills", indexes = {
        @Index(name = "idx_bill_calculator_month", columnList = "calculator_id,year,month")
})
public class Bill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "calculator_id", nullable = false)
    private Long calculatorId;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer month;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal waterBill = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal electricBill = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal gasBill = BigDecimal.ZERO;

    private Integer totalDays;

    private Integer ownerDays;

    @Column(length = 50)
    private String ruleName;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "json")
    private List<ShareResult> shares;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
