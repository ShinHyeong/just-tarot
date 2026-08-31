package com.justtarot.tarot_reading.dto.request;

import com.justtarot.tarot_reading.dto.DrawnCard;
import com.justtarot.tarot_reading.dto.analysis.QuestionAnalysis;

import java.util.List;

/**
 * Interpreter에 보낼 확정된 Request(질문)
 */
public record PreparedReadingRequest(
        boolean ambiguous,
        List<String> candidates,
        Long readingId,
        String effectiveQuestion,
        QuestionAnalysis questionAnalysis,
        List<DrawnCard> cards
) {
    public static PreparedReadingRequest needsClarification(List<String> candidates) {
        return new PreparedReadingRequest(
                true,
                candidates,
                null,
                null,
                null,
                List.of()
        );
    }

    public static PreparedReadingRequest ready(Long readingId,
                                               String effectiveQuestion,
                                               QuestionAnalysis analysis,
                                               List<DrawnCard> drawnCards) {
        return new PreparedReadingRequest(
          false,
                List.of(),
                readingId,
                effectiveQuestion,
                analysis,
                drawnCards
        );
    }
}
