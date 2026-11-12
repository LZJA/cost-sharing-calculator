package com.costsharing.calculator.service;

import com.costsharing.calculator.auth.*;
import com.costsharing.calculator.dto.BillRequest;
import com.costsharing.calculator.dto.PreviewRequest;
import com.costsharing.calculator.entity.Bill;
import com.costsharing.calculator.entity.Calculator;
import com.costsharing.calculator.formula.BillInput;
import com.costsharing.calculator.formula.FormulaEngine;
import com.costsharing.calculator.formula.FeeCatalog;
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
import java.util.Map;

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
                request.getDays() != null ? request.getDays() : request.getOwnerDays(), request.getFees());
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
                request.getDays() != null ? request.getDays() : request.getOwnerDays(), request.getFees());
        List<ShareResult> shares = FormulaEngine.calculate(
                calculator.getConfig(), request.getRuleName(), input);

        Map<String, BigDecimal> fees = FeeCatalog.amounts(calculator.getConfig(), input);
        BigDecimal total = fees.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        Bill bill = new Bill();
        bill.setCalculatorId(calculator.getId());
        bill.setCreatedBy(CurrentAccount.id());
        bill.setCanDelete("OWNER".equals(calculator.getCurrentRole()));
        bill.setYear(request.getYear());
        bill.setMonth(request.getMonth());
        bill.setFees(FeeCatalog.snapshot(calculator.getConfig(), fees));
        bill.setWaterBill(fees.getOrDefault("water", BigDecimal.ZERO));
        bill.setElectricBill(fees.getOrDefault("electric", BigDecimal.ZERO));
        bill.setGasBill(fees.getOrDefault("gas", BigDecimal.ZERO));
        bill.setTotalDays(request.getTotalDays());
        bill.setOwnerDays(input.getOwnerDays());
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
        Calculator calculator = calculatorService.getById(calculatorId);
        return billRepository.findByCalculatorAndRange(
                calculatorId, year, month, startYear, startMonth, endYear, endMonth,
                PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size))))
                .map(bill -> { bill.setCanDelete("OWNER".equals(calculator.getCurrentRole())); return bill; });
    }

    @Transactional
    public void deleteBill(Long id) {
        Bill bill = billRepository.findById(id).orElseThrow(() -> new AccessException(404, "账单不存在或无权访问"));
        calculatorService.requireOwner(bill.getCalculatorId());
        billRepository.delete(bill);
        log.info("删除账单: id={}", id);
    }
}
