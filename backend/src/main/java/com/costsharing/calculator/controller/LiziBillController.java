package com.costsharing.calculator.controller;

import com.costsharing.calculator.dto.ApiResponse;
import com.costsharing.calculator.dto.LiziBillRequest;
import com.costsharing.calculator.entity.LiziBill;
import com.costsharing.calculator.service.LiziBillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 李子账单管理接口
 */
@Slf4j
@RestController
@RequestMapping("/lizi-bills")
@RequiredArgsConstructor
public class LiziBillController {

    private final LiziBillService billService;

    /**
     * 计算并保存账单
     */
    @PostMapping
    public ApiResponse<LiziBill> calculateAndSave(@Valid @RequestBody LiziBillRequest request) {
        try {
            LiziBill bill = billService.calculateAndSave(request);
            return ApiResponse.success(bill);
        } catch (Exception e) {
            log.error("保存账单失败", e);
            return ApiResponse.error("保存账单失败: " + e.getMessage());
        }
    }

    /**
     * 获取指定月份的账单
     */
    @GetMapping("/{year}/{month}")
    public ApiResponse<LiziBill> getBillByMonthAndYear(
            @PathVariable Integer year,
            @PathVariable Integer month) {
        return billService.getBillByMonthAndYear(month, year)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error("账单不存在"));
    }

    /**
     * 获取指定年份的所有账单
     */
    @GetMapping("/year/{year}")
    public ApiResponse<List<LiziBill>> getBillsByYear(@PathVariable Integer year) {
        List<LiziBill> bills = billService.getBillsByYear(year);
        return ApiResponse.success(bills);
    }

    /**
     * 获取最近的账单
     */
    @GetMapping("/recent")
    public ApiResponse<List<LiziBill>> getRecentBills(
            @RequestParam(defaultValue = "10") int limit) {
        List<LiziBill> bills = billService.getRecentBills(limit);
        return ApiResponse.success(bills);
    }

    /**
     * 获取所有账单
     */
    @GetMapping
    public ApiResponse<List<LiziBill>> getAllBills() {
        List<LiziBill> bills = billService.getAllBills();
        return ApiResponse.success(bills);
    }

    /**
     * 删除账单
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteBill(@PathVariable Long id) {
        try {
            billService.deleteBill(id);
            return ApiResponse.success();
        } catch (Exception e) {
            log.error("删除账单失败", e);
            return ApiResponse.error("删除账单失败: " + e.getMessage());
        }
    }
}
