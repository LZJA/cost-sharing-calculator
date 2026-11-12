package com.costsharing.calculator.auth;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface CompanionRepository extends JpaRepository<Companion, Long> {
    boolean existsByCalculatorIdAndAccountId(Long calculatorId, Long accountId);
    List<Companion> findByCalculatorId(Long calculatorId);
    void deleteByCalculatorIdAndAccountId(Long calculatorId, Long accountId);
    void deleteByCalculatorId(Long calculatorId);
}
