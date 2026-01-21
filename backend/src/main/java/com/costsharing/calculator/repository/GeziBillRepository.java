package com.costsharing.calculator.repository;

import com.costsharing.calculator.entity.GeziBill;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 鸽子账单数据访问层
 */
@Repository
public interface GeziBillRepository extends JpaRepository<GeziBill, Long> {

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
    @Query("SELECT g FROM GeziBill g WHERE " +
           "(:year IS NULL OR g.year = :year) AND " +
           "(:month IS NULL OR g.month = :month) AND " +
           "((:startYear IS NULL AND :startMonth IS NULL) OR " +
           " (g.year > :startYear OR (g.year = :startYear AND g.month >= :startMonth))) AND " +
           "((:endYear IS NULL AND :endMonth IS NULL) OR " +
           " (g.year < :endYear OR (g.year = :endYear AND g.month <= :endMonth))) " +
           "ORDER BY g.year DESC, g.month DESC")
    Page<GeziBill> findByYearAndMonth(
            @Param("year") Integer year,
            @Param("month") Integer month,
            @Param("startYear") Integer startYear,
            @Param("startMonth") Integer startMonth,
            @Param("endYear") Integer endYear,
            @Param("endMonth") Integer endMonth,
            Pageable pageable
    );
}
