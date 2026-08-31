package com.justtarot.tarot_reading.domain.card;

import com.justtarot.tarot_reading.dto.CardDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TotalCardsCache {
    private final CardRepository cardRepository;

    private static final int TAROT_DECK_SIZE = 78;

    //List.of()로 불변성 보장
    private volatile List<CardDto> cards = List.of();

    @EventListener(ApplicationReadyEvent.class)
    void warmUp() {
        this.cards = cardRepository.findAll().stream()
                .map(CardDto::from)
                .toList();

        if (cards.size() != TAROT_DECK_SIZE) {
            log.error("[주의] 로드된 타로 카드가 {}장이 아닙니다. DB를 확인해주세요 (현재: {}장)", TAROT_DECK_SIZE, cards.size());
            return;
        }

        log.info("타로 카드 {}장이 성공적으로 메모리에 로드되었습니다", cards.size());
    }

    public List<CardDto> getAll() {
        // 테스트나 warmUp 이전 접근에 대한 방어
        if (cards.isEmpty()) {warmUp();}

        return cards;
    }
}
