package com.justtarot.tarot_reading.dto.reading.request;

public record ReadingCommand(
        String question,
        int drawCount,
        int retryCount,
        String clarification
) {
    //최종적으로 결정된 질문 텍스트
    public String effectiveQuestion() {
        return (clarification == null || clarification.isBlank()) //사용자가 "직접 입력" 선택했지만 빈 칸으로 입력한 경우 방지
                ? question
                : question+"\n[사용자가 덧붙인 설명] "+clarification;
    }
}
