package com.justtarot.tarot_reading.exception;

import com.justtarot.tarot_reading.dto.global.response.StatusCode;
import org.springframework.http.HttpStatus;

public class ReadingAccessDeniedException extends BusinessException {
    public ReadingAccessDeniedException(Long readingId, Long userId) {
        super(StatusCode.READING_ACCESS_DENIED,
                String.format("유저(userId: %d)는 리딩 이력(readingId: %d)에 대한 권한이 없습니다", userId, readingId));
    }

    @Override
    public HttpStatus httpStatus() { return HttpStatus.FORBIDDEN; }
}
