package com.costsharing.calculator.controller;

import com.costsharing.calculator.dto.ApiResponse;
import com.costsharing.calculator.dto.CalculatorRequest;
import com.costsharing.calculator.entity.Calculator;
import com.costsharing.calculator.service.CalculatorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/calculators")
@RequiredArgsConstructor
public class CalculatorController {

    private final CalculatorService calculatorService;

    @GetMapping
    public ApiResponse<List<Calculator>> getAll() {
        return ApiResponse.success(calculatorService.getAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<Calculator> getById(@PathVariable Long id) {
        try {
            return ApiResponse.success(calculatorService.getById(id));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(404, e.getMessage());
        }
    }

    @PostMapping
    public ApiResponse<Calculator> create(@Valid @RequestBody CalculatorRequest request) {
        try {
            return ApiResponse.success(calculatorService.create(request));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ApiResponse<Calculator> update(@PathVariable Long id,
                                          @Valid @RequestBody CalculatorRequest request) {
        try {
            return ApiResponse.success(calculatorService.update(id, request));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        try {
            calculatorService.delete(id);
            return ApiResponse.success();
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
