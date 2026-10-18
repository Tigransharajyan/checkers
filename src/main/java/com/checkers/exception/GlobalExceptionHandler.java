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

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusiness(BusinessException ex) {
        HttpStatus status = mapStatus(ex.getErrorCode());
        return ResponseEntity.status(status).body(ApiErrorResponse.builder()
                .errorCode(ex.getErrorCode().name())
                .timestamp(Instant.now())
                .details(ex.getDetails() == null ? Map.of() : ex.getDetails())
                .build());
    }

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

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResourceFound(org.springframework.web.servlet.resource.NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiErrorResponse.builder()
                .errorCode("NOT_FOUND")
                .timestamp(Instant.now())
                .details(Map.of("resourcePath", ex.getResourcePath()))
                .build());
    }

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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex) {
        org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class).error("Unhandled exception in API request: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiErrorResponse.builder()
                .errorCode(ErrorCode.INTERNAL_ERROR.name())
                .timestamp(Instant.now())
                .details(Map.of("exception", ex.getClass().getSimpleName()))
                .build());
    }

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
