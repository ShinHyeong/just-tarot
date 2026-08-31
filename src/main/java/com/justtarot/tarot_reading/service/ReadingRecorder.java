package com.justtarot.tarot_reading.service;

import com.justtarot.tarot_reading.domain.reading.Reading;
import com.justtarot.tarot_reading.domain.reading.ReadingCard;
import com.justtarot.tarot_reading.domain.reading.ReadingCardRepository;
import com.justtarot.tarot_reading.domain.reading.ReadingRepository;
import com.justtarot.tarot_reading.dto.DrawnCard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ReadingRecorder {
    private final ReadingRepository readingRepository;
    private final ReadingCardRepository readingCardRepository;

    /**
     * 유저별 질문과 뽑은 카드 이력을 기록한다
     */
    @Transactional
    public Long record(Long userId, String question, List<DrawnCard> drawnCards) {
        Reading reading = readingRepository.save(new Reading(userId, question));

        List<ReadingCard> readingCards = drawnCards.stream()
                .map(drawnCard -> new ReadingCard(
                        reading.getId(),
                        drawnCard.cardDto().id(),
                        drawnCard.reversed()
                ))
                .toList();
        readingCardRepository.saveAll(readingCards);

        return reading.getId();
    }
}
