package com.costsharing.calculator.service;

import com.costsharing.calculator.dto.BillRequest;
import com.costsharing.calculator.dto.PreviewRequest;
import com.costsharing.calculator.entity.Bill;
import com.costsharing.calculator.entity.Calculator;
import com.costsharing.calculator.formula.*;
import com.costsharing.calculator.repository.BillRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import com.costsharing.calculator.auth.*;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BillServiceTest {
    @BeforeEach void authenticate() {
        var request = new MockHttpServletRequest(); var account = new Account(); account.setId(1L);
        request.setAttribute(CurrentAccount.ATTRIBUTE, account);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
    @AfterEach void clearIdentity() { RequestContextHolder.resetRequestAttributes(); }
    @Test
    void savesCustomFeeSnapshotsAndUsesSameResultAsPreview() {
        var repository = mock(BillRepository.class);
        var calculators = mock(CalculatorService.class);
        var service = new BillService(repository, calculators);
        var calculator = new Calculator();
        calculator.setId(1L);
        var config = DefaultConfigs.lizi();
        config.setFees(List.of(new FeeConfig("trip", "交通费", "🚕")));
        calculator.setConfig(config);
        when(calculators.getById(1L)).thenReturn(calculator);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var request = new BillRequest();
        request.setCalculatorId(1L);
        request.setYear(2026);
        request.setMonth(11);
        request.setFees(Map.of("trip", new BigDecimal("300")));
        request.setTotalDays(90);
        request.setDays(45);
        Bill saved = service.calculateAndSave(request);
        assertEquals(new BigDecimal("300.00"), saved.getTotalAmount());
        assertEquals("交通费", saved.getFees().get(0).getName());
        assertEquals(45, saved.getOwnerDays());
        assertEquals(BigDecimal.ZERO, saved.getWaterBill());
        var preview = new PreviewRequest();
        preview.setConfig(config);
        preview.setFees(request.getFees());
        preview.setTotalDays(90);
        preview.setDays(45);
        assertEquals(saved.getShares(), service.preview(preview));
        config.getFees().get(0).setName("打车费");
        assertEquals("交通费", saved.getFees().get(0).getName());
    }
}
