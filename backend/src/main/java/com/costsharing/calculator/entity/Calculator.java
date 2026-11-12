package com.costsharing.calculator.entity;

import com.costsharing.calculator.formula.CalculatorConfig;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * 一个可配置的分账计算器。人员和规则都在 config 里，改名字不用发版。
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "calculators")
public class Calculator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 200)
    private String description;

    @Column(length = 10)
    private String avatar;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String background;

    @Column(nullable = false)
    private Boolean enableBackground = true;

    private Long ownerId;

    @Transient private String currentRole;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "json")
    private CalculatorConfig config;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
