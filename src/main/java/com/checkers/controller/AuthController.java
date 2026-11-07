package com.checkers.controller;

import com.checkers.dto.request.LoginRequest;
import com.checkers.dto.request.RefreshTokenRequest;
import com.checkers.dto.request.RegisterRequest;
import com.checkers.dto.response.AuthResponse;
import com.checkers.dto.response.UserResponse;
import com.checkers.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST contract for registration, login, guest sessions and tokens. AuthService performs account
 * operations.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Accepts registration data after Bean Validation and delegates account creation and token
     * issuance to AuthService.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    /**
     * Passes the username or email and password to AuthService. Returns tokens only after
     * successful credential verification.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Creates a guest session without email or password so a visitor can start playing without
     * registration.
     */
    @PostMapping("/guest")
    public ResponseEntity<AuthResponse> guest() {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.guestLogin());
    }

    /**
     * Exchanges a valid refresh token for a new access/refresh pair through AuthService.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    /**
     * Revokes the supplied refresh token and completes the request without a response body.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody(required = false) RefreshTokenRequest request) {
        if (request != null && request.getRefreshToken() != null) {
            authService.logout(request);
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * Returns safe information about the authenticated user. Neither the plaintext password nor its
     * hash is included.
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me() {
        return ResponseEntity.ok(authService.currentUser());
    }
}
