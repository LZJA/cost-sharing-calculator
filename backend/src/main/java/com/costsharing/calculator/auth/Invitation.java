package com.costsharing.calculator.auth;
import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
@Data @Entity @Table(name="calculator_invitations")
public class Invitation {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private Long calculatorId;
    @JsonIgnore @Column(nullable=false,unique=true,length=64) private String tokenHash;
    @Column(nullable=false) private Instant expiresAt;
    @Column(nullable=false) private boolean revoked;
    private Long acceptedBy;
}
