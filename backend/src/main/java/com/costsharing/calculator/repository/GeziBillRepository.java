package com.costsharing.calculator.repository;

import com.costsharing.calculator.entity.GeziBill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 鸽子账单数据访问层
 */
@Repository
public interface GeziBillRepository extends JpaRepository<GeziBill, Long> {

    /**
     * 查找最近的账单
     *
     * @return 账单列表
     */
    @Query("SELECT g FROM GeziBill g ORDER BY g.createdAt DESC")
    List<GeziBill> findRecentBills();
}
