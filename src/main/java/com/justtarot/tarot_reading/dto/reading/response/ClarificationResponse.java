package com.justtarot.tarot_reading.dto.reading.response;

/**
 * 질문이 모호할 경우 클라이언트에게 보낼 정보
 * @param candidate 사용자에게 물어볼 가장 유력한 질문 의도 후보
 */
public record ClarificationResponse(
        String candidate
) {}
