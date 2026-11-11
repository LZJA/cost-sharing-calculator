package com.costsharing.calculator.service;

import com.costsharing.calculator.entity.Bill;
import com.costsharing.calculator.entity.Calculator;
import com.costsharing.calculator.entity.Card;
import com.costsharing.calculator.entity.GeziBill;
import com.costsharing.calculator.entity.LiziBill;
import com.costsharing.calculator.formula.DefaultConfigs;
import com.costsharing.calculator.formula.ShareResult;
import com.costsharing.calculator.repository.BillRepository;
import com.costsharing.calculator.repository.CalculatorRepository;
import com.costsharing.calculator.repository.CardRepository;
import com.costsharing.calculator.repository.GeziBillRepository;
import com.costsharing.calculator.repository.LiziBillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 启动时把旧的李子、鸽子账单搬进通用账单表。
 * 每个来源只搬一次：对应计算器已经有账单就跳过。旧表保留不删。
 * 金额直接用旧列里存好的数，不重新计算，避免四舍五入和历史算法不一致。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BillMigrationRunner implements ApplicationRunner {

    private final CalculatorRepository calculatorRepository;
    private final BillRepository billRepository;
    private final LiziBillRepository liziBillRepository;
    private final GeziBillRepository geziBillRepository;
    private final CardRepository cardRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        migrateLizi();
        migrateGezi();
    }

    private void migrateLizi() {
        Calculator calculator = calculatorRepository.findByLegacyKey("lizi")
                .orElseGet(() -> createCalculator("lizi", "李子的分账计算器", "分账让生活更简单", "🌰"));
        if (billRepository.countByCalculatorId(calculator.getId()) > 0) {
            log.info("李子账单已迁移过，跳过");
            return;
        }

        List<LiziBill> source = liziBillRepository.findAll();
        BigDecimal oldTotal = BigDecimal.ZERO;
        BigDecimal newTotal = BigDecimal.ZERO;
        for (LiziBill old : source) {
            Bill bill = baseBill(calculator.getId(), old.getYear(), old.getMonth(),
                    old.getWaterBill(), old.getElectricBill(), old.getGasBill(), old.getTotalAmount());
            bill.setTotalDays(old.getTotalDays());
            bill.setOwnerDays(old.getOwnerDays());
            bill.setRuleName("默认分账");
            bill.setShares(List.of(
                    share("owner", "房主", "👤", old.getOwnerAmount()),
                    share("sister", "谢林珠", "🌸", old.getSisterAmount()),
                    share("datou", "张锦豪", "🌻", old.getDatouAmount())
            ));
            billRepository.save(bill);
            oldTotal = oldTotal.add(zero(old.getTotalAmount()));
            newTotal = newTotal.add(sumShares(bill));
        }
        log.info("迁移李子账单 {} 条，旧总费用 {}，新分摊合计 {}", source.size(), oldTotal, newTotal);
        if (oldTotal.compareTo(newTotal) != 0) {
            log.warn("李子账单迁移后总额不一致：旧 {}，新 {}", oldTotal, newTotal);
        }
    }

    private void migrateGezi() {
        Calculator calculator = calculatorRepository.findByLegacyKey("gezi")
                .orElseGet(() -> createCalculator("gezi", "鸽子的分账计算器", "记录每一份美好小账单", "🕊️"));
        if (billRepository.countByCalculatorId(calculator.getId()) > 0) {
            log.info("鸽子账单已迁移过，跳过");
            return;
        }

        List<GeziBill> source = geziBillRepository.findAll();
        BigDecimal oldTotal = BigDecimal.ZERO;
        BigDecimal newTotal = BigDecimal.ZERO;
        for (GeziBill old : source) {
            Bill bill = baseBill(calculator.getId(), old.getYear(), old.getMonth(),
                    old.getWaterBill(), old.getElectricBill(), old.getGasBill(), old.getTotalAmount());
            bill.setRuleName(old.getSplitRule());
            bill.setShares(List.of(
                    share("lizi", "李子", "🍐", old.getLiziAmount()),
                    share("gezi", "鸽子", "🕊️", old.getGeziAmount()),
                    share("chunfeng", "春风", "🌸", old.getChunfengAmount()),
                    share("chengzi", "橙子", "🍊", old.getChengziAmount())
            ));
            billRepository.save(bill);
            oldTotal = oldTotal.add(zero(old.getTotalAmount()));
            newTotal = newTotal.add(sumShares(bill));
        }
        log.info("迁移鸽子账单 {} 条，旧总费用 {}，新分摊合计 {}", source.size(), oldTotal, newTotal);
        if (oldTotal.compareTo(newTotal) != 0) {
            log.warn("鸽子账单迁移后总额不一致：旧 {}，新 {}", oldTotal, newTotal);
        }
    }

    private Calculator createCalculator(String legacyKey, String defaultName, String defaultDescription, String defaultAvatar) {
        Card card = cardRepository.findByType(legacyKey).orElse(null);
        Calculator calculator = new Calculator();
        calculator.setLegacyKey(legacyKey);
        calculator.setName(card != null && card.getName() != null ? card.getName() : defaultName);
        calculator.setDescription(card != null ? card.getDescription() : defaultDescription);
        calculator.setAvatar(card != null && card.getAvatar() != null ? card.getAvatar() : defaultAvatar);
        calculator.setBackground(card != null ? card.getBackground() : "");
        calculator.setEnableBackground(card != null && card.getEnableBackground() != null
                ? card.getEnableBackground() : true);
        calculator.setConfig("lizi".equals(legacyKey) ? DefaultConfigs.lizi() : DefaultConfigs.gezi());
        return calculatorRepository.save(calculator);
    }

    private Bill baseBill(Long calculatorId, Integer year, Integer month,
                          BigDecimal water, BigDecimal electric, BigDecimal gas, BigDecimal total) {
        Bill bill = new Bill();
        bill.setCalculatorId(calculatorId);
        bill.setYear(year == null ? 0 : year);
        bill.setMonth(month == null ? 0 : month);
        bill.setWaterBill(zero(water));
        bill.setElectricBill(zero(electric));
        bill.setGasBill(zero(gas));
        bill.setTotalAmount(zero(total));
        return bill;
    }

    private ShareResult share(String key, String name, String emoji, BigDecimal amount) {
        return new ShareResult(key, name, emoji, zero(amount));
    }

    private BigDecimal sumShares(Bill bill) {
        return bill.getShares().stream()
                .map(ShareResult::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
