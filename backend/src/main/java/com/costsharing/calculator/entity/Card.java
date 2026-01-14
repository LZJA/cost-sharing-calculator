package com.costsharing.calculator.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 卡片配置实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cards")
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 卡片类型: lizi 或 gezi
     */
    @Column(nullable = false, unique = true, length = 10)
    private String type;

    /**
     * 卡片名称
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * 描述语
     */
    @Column(length = 200)
    private String description;

    /**
     * 头像（emoji）
     */
    @Column(length = 10)
    private String avatar;

    /**
     * 背景图片路径
     */
    @Column(length = 500)
    private String background;

    /**
     * 是否启用背景图
     */
    @Column(nullable = false)
    private Boolean enableBackground = true;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
