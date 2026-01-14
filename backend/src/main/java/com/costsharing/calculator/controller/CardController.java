package com.costsharing.calculator.controller;

import com.costsharing.calculator.dto.ApiResponse;
import com.costsharing.calculator.dto.CardRequest;
import com.costsharing.calculator.entity.Card;
import com.costsharing.calculator.service.CardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 卡片管理接口
 */
@Slf4j
@RestController
@RequestMapping("/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    /**
     * 获取所有卡片
     */
    @GetMapping
    public ApiResponse<List<Card>> getAllCards() {
        List<Card> cards = cardService.getAllCards();
        return ApiResponse.success(cards);
    }

    /**
     * 根据类型获取卡片
     */
    @GetMapping("/{type}")
    public ApiResponse<Card> getCardByType(@PathVariable String type) {
        return cardService.getCardByType(type)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error("卡片不存在"));
    }

    /**
     * 保存或更新卡片
     */
    @PostMapping
    public ApiResponse<Card> saveOrUpdateCard(@Valid @RequestBody CardRequest request) {
        try {
            Card card = cardService.saveOrUpdateCard(request);
            return ApiResponse.success(card);
        } catch (Exception e) {
            log.error("保存卡片失败", e);
            return ApiResponse.error("保存卡片失败: " + e.getMessage());
        }
    }
}
