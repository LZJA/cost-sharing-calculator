package com.costsharing.calculator.auth;

import com.costsharing.calculator.repository.CalculatorRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 仅信任服务端部署配置，绝不按昵称或首个访问者认领历史数据。 */
@Service
public class LegacyOwnership {
    private final CalculatorRepository calculators;
    private final String ownerOpenid;
    private final AccountRepository accounts;
    public LegacyOwnership(CalculatorRepository calculators, @Value("${LEGACY_OWNER_OPENID:}") String ownerOpenid, AccountRepository accounts) {
        this.accounts=accounts;
        this.calculators=calculators; this.ownerOpenid=ownerOpenid;
    }
    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    @Transactional
    public void assignExistingAccount() {
        if (!ownerOpenid.isBlank()) accounts.findByOpenidHash(Tokens.hash(ownerOpenid)).ifPresent(this::assign);
    }
    @Transactional
    public void assign(Account account) {
        if (ownerOpenid.isBlank() || !ownerOpenid.equals(account.getOpenid())) return;
        calculators.assignUnowned(account.getId());
    }
}
