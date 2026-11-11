package com.costsharing.calculator.repository;

import com.costsharing.calculator.entity.Bill;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {

    long countByCalculatorId(Long calculatorId);

    @Query("SELECT b FROM Bill b WHERE b.calculatorId = :calculatorId AND " +
           "(:year IS NULL OR b.year = :year) AND " +
           "(:month IS NULL OR b.month = :month) AND " +
           "((:startYear IS NULL AND :startMonth IS NULL) OR " +
           " (b.year > :startYear OR (b.year = :startYear AND b.month >= :startMonth))) AND " +
           "((:endYear IS NULL AND :endMonth IS NULL) OR " +
           " (b.year < :endYear OR (b.year = :endYear AND b.month <= :endMonth))) " +
           "ORDER BY b.year DESC, b.month DESC, b.id DESC")
    Page<Bill> findByCalculatorAndRange(
            @Param("calculatorId") Long calculatorId,
            @Param("year") Integer year,
            @Param("month") Integer month,
            @Param("startYear") Integer startYear,
            @Param("startMonth") Integer startMonth,
            @Param("endYear") Integer endYear,
            @Param("endMonth") Integer endMonth,
            Pageable pageable
    );
}
