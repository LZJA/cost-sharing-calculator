package com.costsharing.calculator.auth;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
public final class CurrentAccount {
    public static final String ATTRIBUTE = "authenticatedAccount";
    private CurrentAccount() {}
    public static Account account() {
        var attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servlet &&
                servlet.getRequest().getAttribute(ATTRIBUTE) instanceof Account account) return account;
        throw new AccessException(401, "请重新登录");
    }
    public static Long id() { return account().getId(); }
}
