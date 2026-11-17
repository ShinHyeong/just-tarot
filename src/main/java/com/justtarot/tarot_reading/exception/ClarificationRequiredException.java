package com.justtarot.tarot_reading.exception;

import com.justtarot.tarot_reading.dto.global.response.StatusCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ClarificationRequiredException extends BusinessException {
    private final String candidate;

    public ClarificationRequiredException(String candidate) {
        super(StatusCode.CLARIFICATION_REQUIRED, "질문이 모호합니다.");
        this.candidate = candidate;
    }

    @Override
    public HttpStatus httpStatus() { return HttpStatus.UNPROCESSABLE_CONTENT; }
}
