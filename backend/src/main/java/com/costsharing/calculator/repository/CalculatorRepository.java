package com.costsharing.calculator.repository;

import com.costsharing.calculator.entity.Calculator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CalculatorRepository extends JpaRepository<Calculator, Long> {

    Optional<Calculator> findByLegacyKey(String legacyKey);
}
