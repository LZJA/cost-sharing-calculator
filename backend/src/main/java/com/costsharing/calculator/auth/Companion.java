package com.costsharing.calculator.auth;
import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;
@Data @Entity @Table(name="calculator_companions", uniqueConstraints=@UniqueConstraint(columnNames={"calculator_id","account_id"}))
public class Companion {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="calculator_id",nullable=false) private Long calculatorId;
    @Column(name="account_id",nullable=false) private Long accountId;
    @Column(nullable=false) private Instant joinedAt = Instant.now();
}
