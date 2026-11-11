package com.costsharing.calculator.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 独立于计算器和账单保存迁移标记，删除业务数据不会触发重新导入。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "bill_migrations")
public class BillMigration {

    @Id
    @Column(length = 10)
    private String source;
}
