package com.justtarot.tarot_reading.exception;

import com.justtarot.tarot_reading.dto.global.response.StatusCode;
import org.springframework.http.HttpStatus;

/**
 * 회원·구독자 이용 제한(주간 무료 한도·결제 주기 한도·속도 제한)에 걸린 요청. 사유는 StatusCode 로 구분한다.
 * 게스트는 {@link GuestAccessBlockedException} 을 쓴다.
 */
public class UsageLimitExceededException extends BusinessException {
    public UsageLimitExceededException(StatusCode statusCode) {
        super(statusCode, statusCode.getMessage());
    }

    @Override
    public HttpStatus httpStatus() { return HttpStatus.TOO_MANY_REQUESTS; }
}
