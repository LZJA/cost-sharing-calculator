package com.costsharing.calculator.auth;
import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;
@Data @Entity @Table(name="login_sessions", indexes=@Index(name="idx_session_expiry",columnList="expires_at"))
public class LoginSession {
    @Id @Column(length=64) private String tokenHash;
    @Column(nullable=false) private Long accountId;
    @Column(nullable=false) private Instant expiresAt;
}
