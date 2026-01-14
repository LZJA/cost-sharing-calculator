package com.costsharing.calculator.controller;

import com.costsharing.calculator.dto.ApiResponse;
import com.costsharing.calculator.dto.GeziBillRequest;
import com.costsharing.calculator.entity.GeziBill;
import com.costsharing.calculator.service.GeziBillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 鸽子账单管理接口
 */
@Slf4j
@RestController
@RequestMapping("/gezi-bills")
@RequiredArgsConstructor
public class GeziBillController {

    private final GeziBillService billService;

    /**
     * 计算并保存账单
     */
    @PostMapping
    public ApiResponse<GeziBill> calculateAndSave(@Valid @RequestBody GeziBillRequest request) {
        try {
            GeziBill bill = billService.calculateAndSave(request);
            return ApiResponse.success(bill);
        } catch (Exception e) {
            log.error("保存账单失败", e);
            return ApiResponse.error("保存账单失败: " + e.getMessage());
        }
    }

    /**
     * 获取最近的账单
     */
    @GetMapping("/recent")
    public ApiResponse<List<GeziBill>> getRecentBills(
            @RequestParam(defaultValue = "10") int limit) {
        List<GeziBill> bills = billService.getRecentBills(limit);
        return ApiResponse.success(bills);
    }

    /**
     * 获取所有账单
     */
    @GetMapping
    public ApiResponse<List<GeziBill>> getAllBills() {
        List<GeziBill> bills = billService.getAllBills();
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
