package com.costsharing.calculator.auth;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface InvitationRepository extends JpaRepository<Invitation,Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Invitation i where i.tokenHash = :hash")
    Optional<Invitation> lockByHash(@org.springframework.data.repository.query.Param("hash") String hash);
    Optional<Invitation> findByTokenHash(String hash);
    @Query("select i.calculatorId from Invitation i where i.tokenHash=:hash")
    Optional<Long> findCalculatorIdByHash(@org.springframework.data.repository.query.Param("hash") String hash);
    List<Invitation> findByCalculatorIdOrderByIdDesc(Long calculatorId);
    void deleteByCalculatorId(Long calculatorId);
    @Modifying(clearAutomatically=true, flushAutomatically=true) @Query("update Invitation i set i.revoked=true where i.calculatorId=:id and i.revoked=false")
    void revokeAll(@org.springframework.data.repository.query.Param("id") Long id);
}
