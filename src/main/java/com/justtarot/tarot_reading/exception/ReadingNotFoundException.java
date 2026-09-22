package com.justtarot.tarot_reading.exception;

import com.justtarot.tarot_reading.dto.global.response.StatusCode;
import org.springframework.http.HttpStatus;

public class ReadingNotFoundException extends BusinessException {
    public ReadingNotFoundException(Long readingId) {
        super(StatusCode.READING_NOT_FOUND,
                "해당 reading_id를 찾을 수 없습니다: " + readingId);
    }

    @Override
    public HttpStatus httpStatus() { return HttpStatus.NOT_FOUND; }
}
