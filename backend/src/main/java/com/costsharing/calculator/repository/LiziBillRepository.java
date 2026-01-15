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
     * 分页查询账单列表（支持按年份和月份过滤）
     *
     * @param year 年份（可选）
     * @param month 月份（可选）
     * @param pageable 分页参数
     * @return 分页结果
     */
    @Query("SELECT l FROM LiziBill l WHERE " +
           "(:year IS NULL OR l.year = :year) AND " +
           "(:month IS NULL OR l.month = :month) " +
           "ORDER BY l.year DESC, l.month DESC")
    Page<LiziBill> findByYearAndMonth(
            @Param("year") Integer year,
            @Param("month") Integer month,
            Pageable pageable
    );
}
