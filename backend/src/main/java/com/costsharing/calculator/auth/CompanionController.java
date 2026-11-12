package com.costsharing.calculator.auth;

import com.costsharing.calculator.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController @RequiredArgsConstructor
public class CompanionController {
    private final CompanionService companions;
    @GetMapping("/calculators/{id}/companions") public ApiResponse<CompanionService.Group> list(@PathVariable Long id) { return ApiResponse.success(companions.list(id)); }
    @PostMapping("/calculators/{id}/invitations") public ApiResponse<CompanionService.InviteLink> invite(@PathVariable Long id) { return ApiResponse.success(companions.invite(id)); }
    @DeleteMapping("/calculators/{id}/invitations/{inviteId}") public ApiResponse<Void> revoke(@PathVariable Long id, @PathVariable Long inviteId) { companions.revoke(id,inviteId); return ApiResponse.success(); }
    @DeleteMapping("/calculators/{id}/companions/{accountId}") public ApiResponse<Void> remove(@PathVariable Long id, @PathVariable Long accountId) { companions.remove(id,accountId); return ApiResponse.success(); }
    @GetMapping("/invitations/{token}") public ApiResponse<CompanionService.InvitePreview> preview(@PathVariable String token) { return ApiResponse.success(companions.preview(token)); }
    @PostMapping("/invitations/{token}/accept") public ApiResponse<Long> accept(@PathVariable String token) { return ApiResponse.success(companions.accept(token)); }
}
