package com.justtarot.tarot_reading.dto.reading.response;

import com.justtarot.tarot_reading.dto.reading.request.ReadingRequest;

/**
 * 질문이 모호할 경우 클라이언트에게 보낼 정보
 * @param question 2차 요청에 그대로 쓸 값을 돌려줌
 * @param drawCount 2차 요청에 그대로 쓸 값을 돌려줌
 * @param candidate 사용자에게 물어볼 가장 유력한 질문 의도 후보
 */
public record ClarificationResponse(
        String type,
        String question,
        int drawCount,
        String candidate
) {
    public static ClarificationResponse of(ReadingRequest request, String candidate) {
        return new ClarificationResponse(
                "CLARIFICATION",
                request.question(),
                request.drawCount(),
                candidate
        );
    }
}
