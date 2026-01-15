package com.costsharing.calculator.service;

import com.costsharing.calculator.dto.GeziBillRequest;
import com.costsharing.calculator.entity.GeziBill;
import com.costsharing.calculator.repository.GeziBillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 鸽子账单服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeziBillService {

    private final GeziBillRepository billRepository;

    /**
     * 计算并保存账单
     */
    @Transactional
    public GeziBill calculateAndSave(GeziBillRequest request) {
        BigDecimal waterBill = request.getWaterBill();
        BigDecimal electricBill = request.getElectricBill();
        BigDecimal gasBill = request.getGasBill();
        String splitRule = request.getSplitRule();

        // 计算总费用
        BigDecimal totalAmount = waterBill.add(electricBill).add(gasBill);

        BigDecimal liziAmount = BigDecimal.ZERO;
        BigDecimal geziAmount = BigDecimal.ZERO;
        BigDecimal chunfengAmount = BigDecimal.ZERO;
        BigDecimal chengziAmount = BigDecimal.ZERO;

        if ("普通分账".equals(splitRule)) {
            // 普通分账规则
            // 水费和燃气费：每人承担1/4
            BigDecimal waterAndGasPerPerson = waterBill.add(gasBill)
                    .divide(new BigDecimal(4), 2, RoundingMode.HALF_UP);
            liziAmount = liziAmount.add(waterAndGasPerPerson);
            geziAmount = geziAmount.add(waterAndGasPerPerson);
            chunfengAmount = chunfengAmount.add(waterAndGasPerPerson);
            chengziAmount = chengziAmount.add(waterAndGasPerPerson);

            // 电费：春风和橙子各承担1/3，李子和鸽子各承担1/6
            liziAmount = liziAmount.add(electricBill.divide(new BigDecimal(6), 2, RoundingMode.HALF_UP));
            geziAmount = geziAmount.add(electricBill.divide(new BigDecimal(6), 2, RoundingMode.HALF_UP));
            chunfengAmount = chunfengAmount.add(electricBill.divide(new BigDecimal(3), 2, RoundingMode.HALF_UP));
            chengziAmount = chengziAmount.add(electricBill.divide(new BigDecimal(3), 2, RoundingMode.HALF_UP));

        } else if ("特殊分账".equals(splitRule)) {
            // 特殊分账规则：在普通分账基础上，橙子费用减少一半，减少的钱由李子和鸽子各承担一半

            // 先按普通分账计算
            BigDecimal waterAndGasPerPerson = waterBill.add(gasBill)
                    .divide(new BigDecimal(4), 2, RoundingMode.HALF_UP);
            liziAmount = liziAmount.add(waterAndGasPerPerson);
            geziAmount = geziAmount.add(waterAndGasPerPerson);
            chunfengAmount = chunfengAmount.add(waterAndGasPerPerson);
            chengziAmount = chengziAmount.add(waterAndGasPerPerson);

            liziAmount = liziAmount.add(electricBill.divide(new BigDecimal(6), 2, RoundingMode.HALF_UP));
            geziAmount = geziAmount.add(electricBill.divide(new BigDecimal(6), 2, RoundingMode.HALF_UP));
            chunfengAmount = chunfengAmount.add(electricBill.divide(new BigDecimal(3), 2, RoundingMode.HALF_UP));
            chengziAmount = chengziAmount.add(electricBill.divide(new BigDecimal(3), 2, RoundingMode.HALF_UP));

            // 橙子的费用减少一半
            BigDecimal chengziReduction = chengziAmount.divide(new BigDecimal(2), 2, RoundingMode.HALF_UP);
            chengziAmount = chengziAmount.subtract(chengziReduction);

            // 减少的费用由李子和鸽子各承担一半
            BigDecimal additionalCostPerPerson = chengziReduction.divide(new BigDecimal(2), 2, RoundingMode.HALF_UP);
            liziAmount = liziAmount.add(additionalCostPerPerson);
            geziAmount = geziAmount.add(additionalCostPerPerson);
        }

        // 创建账单对象
        GeziBill bill = new GeziBill();
        bill.setWaterBill(waterBill);
        bill.setElectricBill(electricBill);
        bill.setGasBill(gasBill);
        bill.setSplitRule(splitRule);
        bill.setYear(request.getYear());
        bill.setMonth(request.getMonth());
        bill.setTotalAmount(totalAmount);
        bill.setLiziAmount(liziAmount);
        bill.setGeziAmount(geziAmount);
        bill.setChunfengAmount(chunfengAmount);
        bill.setChengziAmount(chengziAmount);

        log.info("保存鸽子账单: year={}, month={}, splitRule={}, totalAmount={}",
                request.getYear(), request.getMonth(), splitRule, totalAmount);

        return billRepository.save(bill);
    }

    /**
     * 获取所有账单
     */
    public List<GeziBill> getAllBills() {
        return billRepository.findAll();
    }

    /**
     * 分页查询账单列表（支持按年份和月份过滤）
     *
     * @param year 年份（可选）
     * @param month 月份（可选）
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 分页结果
     */
    public Page<GeziBill> getBillsPage(Integer year, Integer month, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return billRepository.findByYearAndMonth(year, month, pageable);
    }

    /**
     * 删除账单
     */
    @Transactional
    public void deleteBill(Long id) {
        log.info("删除鸽子账单: id={}", id);
        billRepository.deleteById(id);
    }
}
