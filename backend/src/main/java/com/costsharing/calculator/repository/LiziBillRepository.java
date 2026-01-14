package com.costsharing.calculator.repository;

import com.costsharing.calculator.entity.LiziBill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 李子账单数据访问层
 */
@Repository
public interface LiziBillRepository extends JpaRepository<LiziBill, Long> {

    /**
     * 查找指定月份的账单
     *
     * @param month 月份
     * @param year  年份
     * @return 账单信息
     */
    Optional<LiziBill> findByMonthAndYear(Integer month, Integer year);

    /**
     * 查找指定年份的所有账单
     *
     * @param year 年份
     * @return 账单列表
     */
    List<LiziBill> findByYearOrderByMonthDesc(Integer year);

    /**
     * 查找最近的账单
     *
     * @return 账单列表
     */
    @Query("SELECT l FROM LiziBill l ORDER BY l.year DESC, l.month DESC")
    List<LiziBill> findRecentBills();
}
