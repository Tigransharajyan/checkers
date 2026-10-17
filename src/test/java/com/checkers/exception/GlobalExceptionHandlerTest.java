package com.checkers.exception;

import com.checkers.dto.response.ApiErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void handleNoResourceFound_returns404NotFound() {
        NoResourceFoundException ex = new NoResourceFoundException(
                org.springframework.http.HttpMethod.GET, "/api/games/nonexistent");

        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleNoResourceFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("NOT_FOUND", response.getBody().getErrorCode());
        assertEquals("/api/games/nonexistent", response.getBody().getDetails().get("resourcePath"));
    }

    @Test
    void handleBadRequest_typeMismatch_returns400BadRequest() {
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "abc", Long.class, "gameId", null, null);

        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleBadRequest(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(ErrorCode.VALIDATION_FAILED.name(), response.getBody().getErrorCode());
    }

    @Test
    void handleBusiness_gameNotFound_returns404() {
        BusinessException ex = new BusinessException(ErrorCode.GAME_NOT_FOUND);

        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleBusiness(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(ErrorCode.GAME_NOT_FOUND.name(), response.getBody().getErrorCode());
    }
}
