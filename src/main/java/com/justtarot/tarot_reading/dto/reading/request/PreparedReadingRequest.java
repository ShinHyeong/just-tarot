package com.justtarot.tarot_reading.dto.reading.request;

import com.justtarot.tarot_reading.dto.reading.DrawnCard;
import com.justtarot.tarot_reading.dto.reading.analysis.QuestionAnalysis;

import java.util.List;

/**
 * Interpreter에 보낼 확정된 Request(질문)
 */
public record PreparedReadingRequest(
        PreparationStatus status,
        String candidate,

        Long readingId,
        String effectiveQuestion,
        QuestionAnalysis questionAnalysis,
        List<DrawnCard> cards
) {
    // 타로 상담 대상이 아닌 요청
    public static PreparedReadingRequest notSupported() {
        return new PreparedReadingRequest(
                PreparationStatus.NOT_SUPPORTED,
                null,
                null,
                null,
                null,
                List.of()
        );
    }

    // 질문이 모호할 경우
    public static PreparedReadingRequest needsClarification(String candidate) {
        return new PreparedReadingRequest(
                PreparationStatus.CLARIFICATION,
                candidate,
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
                PreparationStatus.READY,
                null,
                readingId,
                effectiveQuestion,
                analysis,
                drawnCards
        );
    }
}
