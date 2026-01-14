package com.costsharing.calculator.service;

import com.costsharing.calculator.dto.LiziBillRequest;
import com.costsharing.calculator.entity.LiziBill;
import com.costsharing.calculator.repository.LiziBillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

/**
 * 李子账单服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LiziBillService {

    private final LiziBillRepository billRepository;

    /**
     * 计算并保存账单
     */
    @Transactional
    public LiziBill calculateAndSave(LiziBillRequest request) {
        // 计算总费用
        BigDecimal totalAmount = request.getWaterBill()
                .add(request.getElectricBill())
                .add(request.getGasBill());

        // 计算房主应承担费用
        BigDecimal ownerRatio = new BigDecimal(request.getOwnerDays())
                .divide(new BigDecimal(request.getTotalDays()), 4, RoundingMode.HALF_UP);
        BigDecimal ownerAmount = totalAmount.multiply(ownerRatio)
                .divide(new BigDecimal(2), 2, RoundingMode.HALF_UP);

        // 计算剩余费用
        BigDecimal remainingAmount = totalAmount.subtract(ownerAmount);

        // 计算剩余费用的分摊（谢林珠和张锦豪均摊）
        BigDecimal sisterAmount = remainingAmount.divide(new BigDecimal(2), 2, RoundingMode.HALF_UP);
        BigDecimal datouAmount = remainingAmount.divide(new BigDecimal(2), 2, RoundingMode.HALF_UP);

        // 创建账单对象
        LiziBill bill = new LiziBill();
        bill.setMonth(request.getMonth());
        bill.setYear(request.getYear());
        bill.setTotalDays(request.getTotalDays());
        bill.setWaterBill(request.getWaterBill());
        bill.setElectricBill(request.getElectricBill());
        bill.setGasBill(request.getGasBill());
        bill.setOwnerDays(request.getOwnerDays());
        bill.setTotalAmount(totalAmount);
        bill.setOwnerAmount(ownerAmount);
        bill.setRemainingAmount(remainingAmount);
        bill.setSisterAmount(sisterAmount);
        bill.setDatouAmount(datouAmount);

        log.info("保存李子账单: month={}, year={}, totalAmount={}",
                request.getMonth(), request.getYear(), totalAmount);

        return billRepository.save(bill);
    }

    /**
     * 获取指定月份的账单
     */
    public Optional<LiziBill> getBillByMonthAndYear(Integer month, Integer year) {
        return billRepository.findByMonthAndYear(month, year);
    }

    /**
     * 获取指定年份的所有账单
     */
    public List<LiziBill> getBillsByYear(Integer year) {
        return billRepository.findByYearOrderByMonthDesc(year);
    }

    /**
     * 获取最近的账单
     */
    public List<LiziBill> getRecentBills(int limit) {
        return billRepository.findRecentBills()
                .stream()
                .limit(limit)
                .toList();
    }

    /**
     * 获取所有账单
     */
    public List<LiziBill> getAllBills() {
        return billRepository.findAll();
    }

    /**
     * 删除账单
     */
    @Transactional
    public void deleteBill(Long id) {
        log.info("删除李子账单: id={}", id);
        billRepository.deleteById(id);
    }
}
