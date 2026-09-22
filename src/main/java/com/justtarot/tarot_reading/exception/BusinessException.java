package com.justtarot.tarot_reading.exception;

import com.justtarot.tarot_reading.dto.global.response.StatusCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class BusinessException extends RuntimeException {
    private final StatusCode statusCode;

    protected BusinessException(StatusCode statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public abstract HttpStatus httpStatus();
}
