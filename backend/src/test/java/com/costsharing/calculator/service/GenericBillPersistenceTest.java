package com.costsharing.calculator.service;

import com.costsharing.calculator.dto.BillRequest;
import com.costsharing.calculator.dto.CalculatorRequest;
import com.costsharing.calculator.formula.*;
import com.costsharing.calculator.entity.Bill;
import com.costsharing.calculator.repository.BillRepository;
import com.costsharing.calculator.repository.CalculatorRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(showSql = false, properties = {
        "spring.datasource.url=jdbc:h2:mem:genericfees;MODE=MySQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=year,month",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.sql.init.mode=never",
        "logging.level.org.hibernate.SQL=WARN"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ CalculatorService.class, BillService.class })
class GenericBillPersistenceTest {
    @Autowired CalculatorService calculators;
    @Autowired BillService bills;
    @Autowired BillRepository billRepository;
    @Autowired CalculatorRepository calculatorRepository;
    @Autowired EntityManager entityManager;

    @Test
    void customFeesAndDayLabelsRoundTripAndOldBillKeepsItsSnapshot() {
        var config = DefaultConfigs.lizi();
        config.setFees(new ArrayList<>(List.of(new FeeConfig("service", "服务费", "💰"), new FeeConfig("net", "网费", "🌐"))));
        config.setDayLabels(Map.of("total", "服务周期天数", "counted", "使用天数"));
        var request = new CalculatorRequest();
        request.setName("通用费用测试");
        request.setConfig(config);
        var calculator = calculators.create(request);
        entityManager.flush();
        entityManager.clear();
        var loadedConfig = calculatorRepository.findById(calculator.getId()).orElseThrow().getConfig();
        assertEquals("服务费", loadedConfig.getFees().get(0).getName());
        assertEquals("使用天数", loadedConfig.getDayLabels().get("counted"));

        var billRequest = new BillRequest();
        billRequest.setCalculatorId(calculator.getId());
        billRequest.setYear(2026);
        billRequest.setMonth(11);
        billRequest.setFees(Map.of("service", new BigDecimal("300"), "net", new BigDecimal("60")));
        billRequest.setTotalDays(90);
        billRequest.setDays(45);
        var bill = bills.calculateAndSave(billRequest);
        entityManager.flush();
        entityManager.clear();
        var stored = billRepository.findById(bill.getId()).orElseThrow();
        assertEquals(new BigDecimal("360.00"), stored.getTotalAmount());
        assertEquals(new BigDecimal("60.00"), stored.getShares().get(0).getAmount());
        assertEquals("服务费", stored.getFees().get(0).getName());

        config.setFees(List.of(new FeeConfig("service", "设备租赁费", "🔧")));
        request.setConfig(config);
        calculators.update(calculator.getId(), request);
        entityManager.flush();
        entityManager.clear();
        var oldBill = billRepository.findById(bill.getId()).orElseThrow();
        assertEquals(2, oldBill.getFees().size());
        assertEquals("服务费", oldBill.getFees().get(0).getName());
        assertEquals("网费", oldBill.getFees().get(1).getName());
        assertEquals(new BigDecimal("300.00"), oldBill.getFees().get(0).getAmount());
        assertEquals("设备租赁费", calculatorRepository.findById(calculator.getId()).orElseThrow()
                .getConfig().getFees().get(0).getName());
    }

    @Test
    void oldBillsMayKeepLegacyColumnsWithoutNewFeeSnapshot() {
        Bill old = new Bill();
        old.setCalculatorId(1L);
        old.setYear(2025);
        old.setMonth(11);
        old.setWaterBill(new BigDecimal("10.00"));
        old.setElectricBill(new BigDecimal("20.00"));
        old.setGasBill(new BigDecimal("30.00"));
        old.setTotalAmount(new BigDecimal("60.00"));
        old.setShares(List.of(new ShareResult("a", "A", "👤", new BigDecimal("60.00"))));
        billRepository.saveAndFlush(old);
        entityManager.clear();
        var loaded = billRepository.findById(old.getId()).orElseThrow();
        assertNull(loaded.getFees());
        assertEquals(new BigDecimal("20.00"), loaded.getElectricBill());
        assertEquals(new BigDecimal("60.00"), loaded.getShares().get(0).getAmount());
    }
}
