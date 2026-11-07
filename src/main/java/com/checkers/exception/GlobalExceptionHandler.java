package com.checkers.exception;

import com.checkers.dto.response.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Common REST exception boundary mapping known failures to HTTP statuses and logging unexpected
 * errors.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Converts an expected domain exception into the appropriate HTTP status and standard JSON
     * error with its code, timestamp and details.
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusiness(BusinessException ex) {
        HttpStatus status = mapStatus(ex.getErrorCode());
        return ResponseEntity.status(status).body(ApiErrorResponse.builder()
                .errorCode(ex.getErrorCode().name())
                .timestamp(Instant.now())
                .details(ex.getDetails() == null ? Map.of() : ex.getDetails())
                .build());
    }

    /**
     * Collects Bean Validation failures by form field and returns HTTP 400 in the common API-error
     * format.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> details = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            details.put(fieldError.getField(), fieldError.getCode());
        }
        return ResponseEntity.badRequest().body(ApiErrorResponse.builder()
                .errorCode(ErrorCode.VALIDATION_FAILED.name())
                .timestamp(Instant.now())
                .details(details)
                .build());
    }

    /**
     * Returns HTTP 404 for a missing static resource instead of incorrectly reporting an internal
     * server error.
     */
    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResourceFound(org.springframework.web.servlet.resource.NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiErrorResponse.builder()
                .errorCode("NOT_FOUND")
                .timestamp(Instant.now())
                .details(Map.of("resourcePath", ex.getResourcePath()))
                .build());
    }

    /**
     * Handles malformed parameters, incompatible types and invalid JSON as client errors with HTTP
     * 400.
     */
    @ExceptionHandler({
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
            org.springframework.web.bind.MissingServletRequestParameterException.class,
            org.springframework.http.converter.HttpMessageNotReadableException.class,
            jakarta.validation.ConstraintViolationException.class
    })
    public ResponseEntity<ApiErrorResponse> handleBadRequest(Exception ex) {
        return ResponseEntity.badRequest().body(ApiErrorResponse.builder()
                .errorCode(ErrorCode.VALIDATION_FAILED.name())
                .timestamp(Instant.now())
                .details(Map.of("message", ex.getMessage() != null ? ex.getMessage() : "Invalid request parameter or payload"))
                .build());
    }

    /**
     * Logs an unexpected exception and returns a general HTTP 500 response. Expected domain
     * failures have their own handler.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex) {
        org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class).error("Unhandled exception in API request: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiErrorResponse.builder()
                .errorCode(ErrorCode.INTERNAL_ERROR.name())
                .timestamp(Instant.now())
                .details(Map.of("exception", ex.getClass().getSimpleName()))
                .build());
    }

    /**
     * Maps domain error codes to authentication, authorization, not-found, conflict or bad-request
     * HTTP statuses.
     */
    private HttpStatus mapStatus(ErrorCode code) {
        return switch (code) {
            case UNAUTHORIZED, INVALID_CREDENTIALS, INVALID_REFRESH_TOKEN -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN, GUEST_LOCALE_NOT_PERSISTED, GUEST_HISTORY_FORBIDDEN -> HttpStatus.FORBIDDEN;
            case USER_NOT_FOUND, GAME_NOT_FOUND, TASK_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case USERNAME_TAKEN, EMAIL_TAKEN, INVITE_ALREADY_USED, GAME_ALREADY_STARTED -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}
