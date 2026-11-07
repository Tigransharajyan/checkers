package com.checkers.exception;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * Expected domain failure with an ErrorCode and optional details. GlobalExceptionHandler maps it to
 * the standard API response.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    @Setter
    private Map<String, Object> details = Map.of();

    /**
     * Creates an expected domain failure with an ErrorCode and a server-side exception message. The
     * client chooses the localized display text.
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.name());
        this.errorCode = errorCode;
    }

    /**
     * Creates an expected domain failure with an ErrorCode and a server-side exception message. The
     * client chooses the localized display text.
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
