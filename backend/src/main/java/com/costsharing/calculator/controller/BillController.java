package com.costsharing.calculator.controller;

import com.costsharing.calculator.dto.ApiResponse;
import com.costsharing.calculator.dto.BillRequest;
import com.costsharing.calculator.dto.PreviewRequest;
import com.costsharing.calculator.entity.Bill;
import com.costsharing.calculator.formula.ShareResult;
import com.costsharing.calculator.service.BillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/bills")
@RequiredArgsConstructor
public class BillController {

    private final BillService billService;

    /** 试算，不保存。 */
    @PostMapping("/preview")
    public ApiResponse<List<ShareResult>> preview(@Valid @RequestBody PreviewRequest request) {
        try {
            return ApiResponse.success(billService.preview(request));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping
    public ApiResponse<Bill> calculateAndSave(@Valid @RequestBody BillRequest request) {
        try {
            return ApiResponse.success(billService.calculateAndSave(request));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            log.error("保存账单失败", e);
            return ApiResponse.error("保存账单失败: " + e.getMessage());
        }
    }

    @GetMapping
    public ApiResponse<Page<Bill>> getBillsPage(
            @RequestParam Long calculatorId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer startYear,
            @RequestParam(required = false) Integer startMonth,
            @RequestParam(required = false) Integer endYear,
            @RequestParam(required = false) Integer endMonth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            return ApiResponse.success(billService.getBillsPage(
                    calculatorId, year, month, startYear, startMonth, endYear, endMonth, page, size));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteBill(@PathVariable Long id) {
        try {
            billService.deleteBill(id);
            return ApiResponse.success();
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
