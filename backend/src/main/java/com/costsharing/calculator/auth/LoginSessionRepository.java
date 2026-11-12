package com.costsharing.calculator.auth;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
public interface LoginSessionRepository extends JpaRepository<LoginSession, String> {
    void deleteByExpiresAtBefore(Instant time);
}
