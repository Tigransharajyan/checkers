package com.checkers.exception;

import lombok.Getter;

/**
 * Specialized domain exception for a missing user, game or puzzle.
 */
@Getter
public class ResourceNotFoundException extends BusinessException {

    /**
     * Creates the specialized missing-resource exception using the supplied domain error code.
     */
    public ResourceNotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }
}
