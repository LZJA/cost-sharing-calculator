package com.costsharing.calculator.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service @RequiredArgsConstructor
public class AuthService {
    private final WechatIdentity identity;
    private final AccountRepository accounts;
    private final LoginSessionRepository sessions;
    private final LegacyOwnership ownership;
    private final AccountRegistration registration;
    public record LoginResult(String token, Account account) {}
    @Transactional
    public LoginResult login(String code) {
        String openid = identity.exchange(code);
        Account account;
        try { account=registration.findOrCreate(openid); }
        catch (org.springframework.dao.DataIntegrityViolationException race) {
            account=accounts.findByOpenidHash(Tokens.hash(openid)).orElseThrow(() -> race);
        }
        ownership.assign(account);
        sessions.deleteByExpiresAtBefore(Instant.now());
        String token = Tokens.create();
        var session = new LoginSession(); session.setTokenHash(Tokens.hash(token));
        session.setAccountId(account.getId()); session.setExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS));
        sessions.save(session);
        return new LoginResult(token, account);
    }
    public Account authenticate(String token) {
        if (token == null || token.length() != 43) throw new AccessException(401, "请重新登录");
        var session = sessions.findById(Tokens.hash(token)).filter(s -> s.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new AccessException(401, "登录已过期，请重新登录"));
        return accounts.findById(session.getAccountId()).orElseThrow(() -> new AccessException(401, "请重新登录"));
    }
    @Transactional
    public Account rename(String name) {
        var account = accounts.findById(CurrentAccount.id()).orElseThrow();
        account.setDisplayName(name.trim()); return accounts.save(account);
    }
    @Transactional public void logout(String token) { sessions.deleteById(Tokens.hash(token)); }
}
