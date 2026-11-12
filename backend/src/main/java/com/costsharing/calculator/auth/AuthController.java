package com.costsharing.calculator.auth;

import com.costsharing.calculator.dto.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/auth") @RequiredArgsConstructor
public class AuthController {
    private final AuthService auth;
    public record LoginRequest(@NotBlank @Size(max=256) String code) {}
    public record ProfileRequest(@NotBlank @Size(max=40) String displayName) {}
    @PostMapping("/login") public ApiResponse<AuthService.LoginResult> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(auth.login(request.code()));
    }
    @GetMapping("/me") public ApiResponse<Account> me() { return ApiResponse.success(CurrentAccount.account()); }
    @PutMapping("/me") public ApiResponse<Account> rename(@Valid @RequestBody ProfileRequest request) {
        return ApiResponse.success(auth.rename(request.displayName()));
    }
    @DeleteMapping("/session") public ApiResponse<Void> logout(@RequestHeader("Authorization") String header) {
        auth.logout(header.substring(7)); return ApiResponse.success();
    }
}
