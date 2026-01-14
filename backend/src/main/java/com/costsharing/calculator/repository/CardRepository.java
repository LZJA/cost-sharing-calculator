package com.costsharing.calculator.repository;

import com.costsharing.calculator.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 卡片数据访问层
 */
@Repository
public interface CardRepository extends JpaRepository<Card, Long> {

    /**
     * 根据类型查找卡片
     *
     * @param type 卡片类型（lizi 或 gezi）
     * @return 卡片信息
     */
    Optional<Card> findByType(String type);
}
