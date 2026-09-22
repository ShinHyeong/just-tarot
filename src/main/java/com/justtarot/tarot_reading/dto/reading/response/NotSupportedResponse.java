package com.justtarot.tarot_reading.dto.reading.response;

import com.justtarot.tarot_reading.dto.reading.request.ReadingRequest;

/**
 * 타로 상담으로 답할 수 없는 요청일 때 클라이언트에게 보낼 정보
 * @param drawCount 사용자가 고른 카드 수. 다시 질문할 때 선택을 유지시키기 위해 돌려준다
 * @param message 안내 고정 문구
 */
public record NotSupportedResponse(
        String type,
        int drawCount,
        String message
) {
    private static final String MESSAGE =
            "타로로는 마음이나 지금 처한 상황에 대한 고민을 봐드릴 수 있어요. "
            + "요즘 어떤 게 마음에 걸리시는지 적어주시면 그걸로 봐드릴게요.";

    public static NotSupportedResponse of(ReadingRequest request) {
        return new NotSupportedResponse(
                "NOT_SUPPORTED",
                request.drawCount(),
                MESSAGE
        );
    }
}
