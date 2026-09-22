package com.justtarot.tarot_reading.dto.reading.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReadingRequest(
        @NotBlank(message = "질문은 필수입니다.")
        @Size(max = 255, message = "질문은 255자 이하여야 합니다.")
        String question,

        @Min(value = 1, message = "카드는 최소 1장 이상 뽑아야 합니다.")
        @Max(value = 5, message = "카드는 최대 5장까지 뽑을 수 있습니다.")
        int drawCount,

        //되물음에 대한 응답인지 (CLARIFICATION을 받고 다시 보내는 요청인지)
        boolean clarified,

        //되물음에 대한 사용자의 답: "네"→후보 문장, 직접입력→사용자 문장,
        @Size(max=255, message = "보충 설명은 255자 이하여야 합니다")
        String clarification
) {
    //최종적으로 결정된 질문 텍스트
    public String effectiveQuestion() {
        return (clarification == null || clarification.isBlank()) //사용자가 "직접 입력" 선택했지만 빈 칸으로 입력한 경우 방지
                ? question
                : question+"\n[사용자가 덧붙인 설명] "+clarification;
    }
}
