package com.costsharing.calculator.auth;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import lombok.RequiredArgsConstructor;
@Component @RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {
    private final AuthService auth;
    @Override public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (request.getMethod().equals("OPTIONS")) return true;
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (path.equals("/auth/login") && request.getMethod().equals("POST")) return true;
        String header = request.getHeader("Authorization");
        var account = auth.authenticate(header != null && header.startsWith("Bearer ") ? header.substring(7) : null);
        request.setAttribute(CurrentAccount.ATTRIBUTE, account);
        return true;
    }
}
