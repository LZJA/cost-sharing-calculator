package com.costsharing.calculator.service;

import com.costsharing.calculator.dto.BillRequest;
import com.costsharing.calculator.dto.PreviewRequest;
import com.costsharing.calculator.entity.Bill;
import com.costsharing.calculator.entity.Calculator;
import com.costsharing.calculator.formula.BillInput;
import com.costsharing.calculator.formula.FormulaEngine;
import com.costsharing.calculator.formula.ShareResult;
import com.costsharing.calculator.repository.BillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillService {

    private final BillRepository billRepository;
    private final CalculatorService calculatorService;

    public List<ShareResult> preview(PreviewRequest request) {
        BillInput input = new BillInput(
                request.getWaterBill(),
                request.getElectricBill(),
                request.getGasBill(),
                request.getTotalDays(),
                request.getOwnerDays());
        return FormulaEngine.calculate(request.getConfig(), request.getRuleName(), input);
    }

    @Transactional
    public Bill calculateAndSave(BillRequest request) {
        Calculator calculator = calculatorService.getById(request.getCalculatorId());
        BillInput input = new BillInput(
                request.getWaterBill(),
                request.getElectricBill(),
                request.getGasBill(),
                request.getTotalDays(),
                request.getOwnerDays());
        List<ShareResult> shares = FormulaEngine.calculate(
                calculator.getConfig(), request.getRuleName(), input);

        BigDecimal total = request.getWaterBill()
                .add(request.getElectricBill())
                .add(request.getGasBill());

        Bill bill = new Bill();
        bill.setCalculatorId(calculator.getId());
        bill.setYear(request.getYear());
        bill.setMonth(request.getMonth());
        bill.setWaterBill(request.getWaterBill());
        bill.setElectricBill(request.getElectricBill());
        bill.setGasBill(request.getGasBill());
        bill.setTotalDays(request.getTotalDays());
        bill.setOwnerDays(request.getOwnerDays());
        bill.setRuleName(request.getRuleName());
        bill.setTotalAmount(total);
        bill.setShares(shares);

        log.info("保存账单: calculatorId={}, {}-{}, total={}",
                calculator.getId(), request.getYear(), request.getMonth(), total);
        return billRepository.save(bill);
    }

    public Page<Bill> getBillsPage(Long calculatorId, Integer year, Integer month,
                                   Integer startYear, Integer startMonth,
                                   Integer endYear, Integer endMonth,
                                   int page, int size) {
        calculatorService.getById(calculatorId);
        return billRepository.findByCalculatorAndRange(
                calculatorId, year, month, startYear, startMonth, endYear, endMonth,
                PageRequest.of(page, size));
    }

    @Transactional
    public void deleteBill(Long id) {
        if (!billRepository.existsById(id)) {
            throw new IllegalArgumentException("账单不存在");
        }
        billRepository.deleteById(id);
        log.info("删除账单: id={}", id);
    }
}
