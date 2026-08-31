package com.justtarot.tarot_reading.domain.reading;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReadingCard {
    @EmbeddedId
    private ReadingCardKey readingCardKey;

    private boolean isReversed;

    public ReadingCard(Long readingId, int cardId, boolean isReversed) {
        this.readingCardKey = new ReadingCardKey(readingId, cardId);
        this.isReversed = isReversed;
    }
}
