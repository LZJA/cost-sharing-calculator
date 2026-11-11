package com.costsharing.calculator.service;

import com.costsharing.calculator.entity.*;
import com.costsharing.calculator.formula.CalculatorConfigValidator;
import com.costsharing.calculator.formula.DefaultConfigs;
import com.costsharing.calculator.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BillMigrationRunnerTest {
    private final CalculatorRepository calculators = mock(CalculatorRepository.class);
    private final BillRepository bills = mock(BillRepository.class);
    private final LiziBillRepository lizi = mock(LiziBillRepository.class);
    private final GeziBillRepository gezi = mock(GeziBillRepository.class);
    private final CardRepository cards = mock(CardRepository.class);
    private final BillMigrationRepository migrations = mock(BillMigrationRepository.class);
    private final BillMigrationRunner runner = new BillMigrationRunner(calculators, bills, lizi, gezi, cards, migrations);

    @BeforeEach
    void skipGeziUnlessTested() {
        when(migrations.existsById("gezi")).thenReturn(true);
    }

    @Test
    void completedMigrationDoesNotRecreateDeletedBillsOrCalculator() {
        when(migrations.existsById("lizi")).thenReturn(true);
        runner.run(null);
        verifyNoInteractions(lizi, gezi, bills);
        verify(calculators, never()).save(any());
    }

    @Test
    void previouslyMigratedCalculatorWithZeroBillsGetsMarkerWithoutReimporting() {
        Calculator existing = new Calculator();
        existing.setId(10L);
        when(calculators.findByLegacyKey("lizi")).thenReturn(Optional.of(existing));
        runner.run(null);
        verify(migrations).save(new BillMigration("lizi"));
        verifyNoInteractions(lizi, bills);
    }

    @Test
    void firstMigrationPreservesHistoricalAmountsAndMarksCompletion() {
        LiziBill old = new LiziBill();
        old.setYear(2025);
        old.setMonth(10);
        old.setTotalDays(31);
        old.setOwnerDays(15);
        old.setWaterBill(new BigDecimal("300.00"));
        old.setTotalAmount(new BigDecimal("300.00"));
        old.setOwnerAmount(new BigDecimal("75.00"));
        old.setSisterAmount(new BigDecimal("112.50"));
        old.setDatouAmount(new BigDecimal("112.50"));
        when(lizi.findAll()).thenReturn(List.of(old));
        when(calculators.save(any())).thenAnswer(invocation -> {
            Calculator calculator = invocation.getArgument(0);
            calculator.setId(10L);
            return calculator;
        });
        runner.run(null);
        ArgumentCaptor<Bill> saved = ArgumentCaptor.forClass(Bill.class);
        verify(bills).save(saved.capture());
        assertEquals(new BigDecimal("75.00"), saved.getValue().getShares().get(0).getAmount());
        assertEquals(new BigDecimal("112.50"), saved.getValue().getShares().get(1).getAmount());
        verify(migrations).save(new BillMigration("lizi"));
    }

    @Test
    void emptySourceIsMarkedAndFailedImportIsNotMarked() {
        when(calculators.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(lizi.findAll()).thenReturn(List.of());
        runner.run(null);
        verify(migrations).save(new BillMigration("lizi"));
        clearInvocations(migrations);
        when(lizi.findAll()).thenThrow(new IllegalStateException("database unavailable"));
        assertThrows(IllegalStateException.class, () -> runner.run(null));
        verify(migrations, never()).save(any());
    }

    @Test
    void upgradesOwnerFormulaWithoutChangingOtherRatiosOrHistoricalBills() {
        Calculator existing = new Calculator();
        existing.setConfig(DefaultConfigs.lizi());
        var owner = existing.getConfig().getRules().get(0).getSteps().get(0).getShares().get(0);
        owner.setBasis(CalculatorConfigValidator.STAY_DAYS);
        owner.setWeight(new BigDecimal("0.5"));
        var other = existing.getConfig().getRules().get(0).getSteps().get(1).getShares().get(0);
        other.setWeight(new BigDecimal("2"));
        when(calculators.findByLegacyKey("lizi")).thenReturn(Optional.of(existing));
        when(migrations.existsById("lizi")).thenReturn(true);
        runner.run(null);
        assertEquals(CalculatorConfigValidator.STAY_DAYS_PER_MEMBER, owner.getBasis());
        assertEquals(BigDecimal.ONE, owner.getWeight());
        assertEquals(new BigDecimal("2"), other.getWeight());
        verify(calculators).save(existing);
        verifyNoInteractions(bills);
        assertFalse(DefaultConfigs.upgradeLegacyLiziOwnerRule(existing.getConfig()));
    }

    @Test
    void keepsCustomOwnerFormula() {
        var config = DefaultConfigs.lizi();
        var owner = config.getRules().get(0).getSteps().get(0).getShares().get(0);
        owner.setBasis(CalculatorConfigValidator.STAY_DAYS);
        owner.setWeight(new BigDecimal("0.2"));
        assertFalse(DefaultConfigs.upgradeLegacyLiziOwnerRule(config));
        assertEquals(new BigDecimal("0.2"), owner.getWeight());
    }

    @Test
    void completedRuleUpgradeDoesNotOverwriteLaterUserEdits() {
        Calculator existing = new Calculator();
        existing.setConfig(DefaultConfigs.lizi());
        var owner = existing.getConfig().getRules().get(0).getSteps().get(0).getShares().get(0);
        owner.setBasis(CalculatorConfigValidator.STAY_DAYS);
        owner.setWeight(new BigDecimal("0.5"));
        when(calculators.findByLegacyKey("lizi")).thenReturn(Optional.of(existing));
        when(migrations.existsById("lizi")).thenReturn(true);
        when(migrations.existsById("lizi-rule")).thenReturn(true);
        runner.run(null);
        assertEquals(CalculatorConfigValidator.STAY_DAYS, owner.getBasis());
        assertEquals(new BigDecimal("0.5"), owner.getWeight());
        verify(calculators, never()).save(any());
    }
}
