package com.costsharing.calculator.repository;

import com.costsharing.calculator.entity.LiziBill;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 李子账单数据访问层
 */
@Repository
public interface LiziBillRepository extends JpaRepository<LiziBill, Long> {

    /**
     * 分页查询账单列表（支持按年份、月份或年月区间过滤）
     *
     * @param year 年份（可选，单独使用时查询该年份的所有账单）
     * @param month 月份（可选，需配合year使用）
     * @param startYear 开始年份（可选，用于区间查询）
     * @param startMonth 开始月份（可选，用于区间查询）
     * @param endYear 结束年份（可选，用于区间查询）
     * @param endMonth 结束月份（可选，用于区间查询）
     * @param pageable 分页参数
     * @return 分页结果
     */
    @Query("SELECT l FROM LiziBill l WHERE " +
           "(:year IS NULL OR l.year = :year) AND " +
           "(:month IS NULL OR l.month = :month) AND " +
           "((:startYear IS NULL AND :startMonth IS NULL) OR " +
           " (l.year > :startYear OR (l.year = :startYear AND l.month >= :startMonth))) AND " +
           "((:endYear IS NULL AND :endMonth IS NULL) OR " +
           " (l.year < :endYear OR (l.year = :endYear AND l.month <= :endMonth))) " +
           "ORDER BY l.year DESC, l.month DESC")
    Page<LiziBill> findByYearAndMonth(
            @Param("year") Integer year,
            @Param("month") Integer month,
            @Param("startYear") Integer startYear,
            @Param("startMonth") Integer startMonth,
            @Param("endYear") Integer endYear,
            @Param("endMonth") Integer endMonth,
            Pageable pageable
    );
}
