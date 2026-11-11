package com.costsharing.calculator.service;

import com.costsharing.calculator.dto.CalculatorRequest;
import com.costsharing.calculator.entity.Calculator;
import com.costsharing.calculator.formula.CalculatorConfigValidator;
import com.costsharing.calculator.repository.BillRepository;
import com.costsharing.calculator.repository.CalculatorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CalculatorService {

    private final CalculatorRepository calculatorRepository;
    private final BillRepository billRepository;

    public List<Calculator> getAll() {
        return calculatorRepository.findAll();
    }

    public Calculator getById(Long id) {
        return calculatorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("计算器不存在"));
    }

    @Transactional
    public Calculator create(CalculatorRequest request) {
        CalculatorConfigValidator.validate(request.getConfig());
        Calculator calculator = new Calculator();
        apply(calculator, request);
        log.info("新增计算器: name={}", calculator.getName());
        return calculatorRepository.save(calculator);
    }

    @Transactional
    public Calculator update(Long id, CalculatorRequest request) {
        CalculatorConfigValidator.validate(request.getConfig());
        Calculator calculator = getById(id);
        apply(calculator, request);
        log.info("更新计算器: id={}, name={}", id, calculator.getName());
        return calculatorRepository.save(calculator);
    }

    @Transactional
    public void delete(Long id) {
        Calculator calculator = getById(id);
        long billCount = billRepository.countByCalculatorId(id);
        if (billCount > 0) {
            throw new IllegalArgumentException("该计算器已有 " + billCount + " 条账单，不能删除");
        }
        calculatorRepository.delete(calculator);
        log.info("删除计算器: id={}, name={}", id, calculator.getName());
    }

    private void apply(Calculator calculator, CalculatorRequest request) {
        calculator.setName(request.getName().trim());
        calculator.setDescription(request.getDescription());
        calculator.setAvatar(request.getAvatar());
        calculator.setBackground(request.getBackground());
        calculator.setEnableBackground(
                request.getEnableBackground() != null ? request.getEnableBackground() : true);
        calculator.setConfig(request.getConfig());
    }

}
