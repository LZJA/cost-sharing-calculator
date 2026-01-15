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
     * 分页查询账单列表（支持按年份和月份过滤）
     *
     * @param year 年份（可选）
     * @param month 月份（可选）
     * @param pageable 分页参数
     * @return 分页结果
     */
    @Query("SELECT g FROM GeziBill g WHERE " +
           "(:year IS NULL OR g.year = :year) AND " +
           "(:month IS NULL OR g.month = :month) " +
           "ORDER BY g.createdAt DESC")
    Page<GeziBill> findByYearAndMonth(
            @Param("year") Integer year,
            @Param("month") Integer month,
            Pageable pageable
    );
}
