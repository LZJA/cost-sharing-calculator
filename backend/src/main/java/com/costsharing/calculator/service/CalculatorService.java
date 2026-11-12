package com.costsharing.calculator.service;

import com.costsharing.calculator.auth.*;
import java.util.Objects;
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

    private final CompanionRepository companions;
    private final InvitationRepository invitations;

    public List<Calculator> getAll() {
        return calculatorRepository.findAccessible(CurrentAccount.id()).stream().map(this::withRole).toList();
    }

    public Calculator getById(Long id) {
        Calculator calculator = calculatorRepository.findById(id)
                .orElseThrow(() -> new AccessException(404, "计算器不存在或无权访问"));
        if (!Objects.equals(calculator.getOwnerId(), CurrentAccount.id()) &&
                !companions.existsByCalculatorIdAndAccountId(id, CurrentAccount.id()))
            throw new AccessException(404, "计算器不存在或无权访问");
        return withRole(calculator);
    }

    private Calculator withRole(Calculator calculator) {
        calculator.setCurrentRole(Objects.equals(calculator.getOwnerId(), CurrentAccount.id()) ? "OWNER" : "MEMBER");
        return calculator;
    }

    public Calculator requireOwner(Long id) {
        Calculator calculator = getById(id);
        if (!Objects.equals(calculator.getOwnerId(), CurrentAccount.id()))
            throw new AccessException(403, "仅创建者可管理计算器和同伴");
        return calculator;
    }

    @Transactional
    public Calculator create(CalculatorRequest request) {
        CalculatorConfigValidator.validate(request.getConfig());
        Calculator calculator = new Calculator();
        calculator.setOwnerId(CurrentAccount.id());
        calculator.setCurrentRole("OWNER");
        apply(calculator, request);
        log.info("新增计算器: name={}", calculator.getName());
        return calculatorRepository.save(calculator);
    }

    @Transactional
    public Calculator update(Long id, CalculatorRequest request) {
        CalculatorConfigValidator.validate(request.getConfig());
        Calculator calculator = requireOwner(id);
        apply(calculator, request);
        log.info("更新计算器: id={}, name={}", id, calculator.getName());
        return calculatorRepository.save(calculator);
    }

    @Transactional
    public void delete(Long id) {
        Calculator calculator = requireOwner(id);
        long billCount = billRepository.countByCalculatorId(id);
        if (billCount > 0) {
            throw new IllegalArgumentException("该计算器已有 " + billCount + " 条账单，不能删除");
        }
        companions.deleteByCalculatorId(id);
        invitations.deleteByCalculatorId(id);
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
