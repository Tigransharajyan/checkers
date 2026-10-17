package com.checkers.service;

import com.checkers.dto.request.LoginRequest;
import com.checkers.dto.request.RegisterRequest;
import com.checkers.dto.response.AuthResponse;
import com.checkers.exception.BusinessException;
import com.checkers.exception.ErrorCode;
import com.checkers.model.entity.User;
import com.checkers.repository.RefreshTokenRepository;
import com.checkers.repository.UserRepository;
import com.checkers.security.jwt.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, refreshTokenRepository, passwordEncoder, jwtService);
    }

    @Test
    void register_normalizesEmailToLowercase() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("AliceUser");
        request.setEmail("  Alice.User@Example.COM  ");
        request.setPassword("securePassword123");

        when(userRepository.existsByUsername("AliceUser")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("alice.user@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_pw");
        when(jwtService.createAccessToken(any(User.class))).thenReturn("access_token");
        when(jwtService.createRefreshTokenValue()).thenReturn("refresh_token");
        when(jwtService.refreshExpiry()).thenReturn(java.time.Instant.now().plusSeconds(3600));

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertEquals("alice.user@example.com", saved.getEmail());
        assertEquals("AliceUser", saved.getUsername());
    }

    @Test
    void register_duplicateEmailDifferentCase_throwsEmailTaken() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("BobUser");
        request.setEmail("Bob@Example.com");
        request.setPassword("password123");

        when(userRepository.existsByUsername("BobUser")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("bob@example.com")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.register(request));
        assertEquals(ErrorCode.EMAIL_TAKEN, ex.getErrorCode());
    }

    @Test
    void login_withMixedCaseEmail_usesFindByIdentifierAndSucceeds() {
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("User.One@Domain.COM");
        request.setPassword("validPass");

        User existing = User.builder()
                .username("userone")
                .email("user.one@domain.com")
                .passwordHash("hashedPass")
                .guest(false)
                .build();

        when(userRepository.findByIdentifier("User.One@Domain.COM")).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("validPass", "hashedPass")).thenReturn(true);
        when(jwtService.createAccessToken(existing)).thenReturn("access_token");
        when(jwtService.createRefreshTokenValue()).thenReturn("refresh_token");
        when(jwtService.refreshExpiry()).thenReturn(java.time.Instant.now().plusSeconds(3600));

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("access_token", response.getAccessToken());
    }
}
