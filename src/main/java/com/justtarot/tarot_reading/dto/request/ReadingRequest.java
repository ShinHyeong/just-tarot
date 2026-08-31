package com.justtarot.tarot_reading.dto.request;

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

        //되물음 요청에 대한 사용자의 답(후보 문장 선택 또는 직접 입력)
        @Size(max=255, message = "보충 설명은 255자 이하여야 합니다")
        String clarification
) {
    public boolean isRetry(){ //되묻기 버전인지 확인
        return clarification != null && !clarification.isBlank();
    }

    //최종적으로 결정된 질문 텍스트
    public String effectiveQuestion() {
        return isRetry() ? question+"\n\n[사용자가 덧붙인 설명]\n"+clarification+"\n" : question;
    }
}
