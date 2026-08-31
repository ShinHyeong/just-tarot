package com.justtarot.tarot_reading.dto.response;

import com.justtarot.tarot_reading.dto.request.ReadingRequest;

import java.util.List;

public record ClarificationResponse(
        String type,
        String question,
        int drawCount,
        List<String> candidates
) {
    public static ClarificationResponse of(ReadingRequest request,
                                           List<String> candidates) {
        return new ClarificationResponse(
                "CLARIFICATION",
                request.question(), //질문이 모호할 경우 2차 요청에 그대로 쓸 값을 돌려줌
                request.drawCount(), // 질문이 모호할 경우 2차 요청에 그대로 쓸 값을 돌려줌
                candidates
        );
    }
}
