package com.costsharing.calculator.repository;

import com.costsharing.calculator.entity.Calculator;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CalculatorRepository extends JpaRepository<Calculator, Long> {

    @Query("select c from Calculator c where c.ownerId=:accountId or exists (select m.id from Companion m where m.calculatorId=c.id and m.accountId=:accountId) order by c.id")
    List<Calculator> findAccessible(@Param("accountId") Long accountId);

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Calculator c where c.id=:id")
    Optional<Calculator> lockById(@Param("id") Long id);
}
