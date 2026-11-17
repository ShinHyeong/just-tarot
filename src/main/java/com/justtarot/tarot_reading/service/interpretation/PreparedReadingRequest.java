package com.justtarot.tarot_reading.service.interpretation;

import com.justtarot.tarot_reading.dto.reading.DrawnCard;
import com.justtarot.tarot_reading.dto.reading.analysis.QuestionAnalysis;

import java.util.List;

/**
 * Interpreter에 보낼 확정된 Request(질문)
 */
public record PreparedReadingRequest(
        Long readingId,
        String effectiveQuestion,
        QuestionAnalysis questionAnalysis,
        List<DrawnCard> cards
) {
    public static PreparedReadingRequest ready(Long readingId,
                                               String effectiveQuestion,
                                               QuestionAnalysis analysis,
                                               List<DrawnCard> drawnCards) {
        return new PreparedReadingRequest(
                readingId,
                effectiveQuestion,
                analysis,
                drawnCards
        );
    }
}
