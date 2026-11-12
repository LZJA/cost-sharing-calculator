package com.costsharing.calculator.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service @RequiredArgsConstructor
public class AccountRegistration {
    private final AccountRepository accounts;
    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public Account findOrCreate(String openid) {
        return accounts.findByOpenidHash(Tokens.hash(openid)).orElseGet(() -> {
            var created=new Account(); created.setOpenid(openid); created.setOpenidHash(Tokens.hash(openid)); return accounts.saveAndFlush(created);
        });
    }
}
