package com.justtarot.tarot_reading.exception;

import com.justtarot.tarot_reading.dto.global.response.StatusCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class QuestionNotSupportedException extends BusinessException {
    public QuestionNotSupportedException() {
        super(StatusCode.QUESTION_NOT_SUPPORTED, "본 서비스에서 지원하지 않는 질문입니다.");
    }

    @Override
    public HttpStatus httpStatus() { return HttpStatus.UNPROCESSABLE_CONTENT; }
}
