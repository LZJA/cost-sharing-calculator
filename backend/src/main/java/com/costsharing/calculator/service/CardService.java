package com.costsharing.calculator.service;

import com.costsharing.calculator.dto.CardRequest;
import com.costsharing.calculator.entity.Card;
import com.costsharing.calculator.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 卡片服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;

    /**
     * 获取所有卡片
     */
    public List<Card> getAllCards() {
        return cardRepository.findAll();
    }

    /**
     * 根据类型获取卡片
     */
    public Optional<Card> getCardByType(String type) {
        return cardRepository.findByType(type);
    }

    /**
     * 保存或更新卡片
     */
    @Transactional
    public Card saveOrUpdateCard(CardRequest request) {
        Card card = cardRepository.findByType(request.getType())
                .orElse(new Card());

        card.setType(request.getType());
        card.setName(request.getName());
        card.setDescription(request.getDescription());
        card.setAvatar(request.getAvatar());
        card.setBackground(request.getBackground());
        card.setEnableBackground(request.getEnableBackground() != null ? request.getEnableBackground() : true);

        log.info("保存卡片配置: type={}, name={}", card.getType(), card.getName());
        return cardRepository.save(card);
    }
}
