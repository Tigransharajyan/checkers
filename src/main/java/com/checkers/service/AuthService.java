package com.checkers.service;

import com.checkers.dto.request.LoginRequest;
import com.checkers.dto.request.RefreshTokenRequest;
import com.checkers.dto.request.RegisterRequest;
import com.checkers.dto.response.AuthResponse;
import com.checkers.dto.response.UserResponse;
import com.checkers.exception.BusinessException;
import com.checkers.exception.ErrorCode;
import com.checkers.exception.ResourceNotFoundException;
import com.checkers.mapper.DtoMapper;
import com.checkers.model.entity.RefreshToken;
import com.checkers.model.entity.User;
import com.checkers.repository.RefreshTokenRepository;
import com.checkers.repository.UserRepository;
import com.checkers.security.CustomUserDetailsService;
import com.checkers.security.SecurityUser;
import com.checkers.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Transactional account registration, login and token renewal. Checks uniqueness, stores password
 * hashes and rotates refresh tokens.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * Normalizes username and email, checks uniqueness, hashes the password with BCrypt and
     * persists the account. Issues access and refresh tokens within the same transaction.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = request.getUsername().trim();
        // Email matching is case-insensitive; Locale.ROOT avoids dependence on the server's
        // language.
        String email = request.getEmail().trim().toLowerCase(java.util.Locale.ROOT);
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException(ErrorCode.USERNAME_TAKEN);
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException(ErrorCode.EMAIL_TAKEN);
        }

        User user = User.builder()
                .username(username)
                .email(email)
                // Persist only a one-way BCrypt hash, never the original password.
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .guest(false)
                .build();
        userRepository.save(user);
        return issueTokens(user);
    }

    /**
     * Finds an account by username or email and verifies the password against its hash. Guests
     * cannot use password login; successful authentication issues a new token pair.
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String identifier = request.getUsernameOrEmail().trim();
        User user = userRepository
                .findByIdentifier(identifier)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        if (user.isGuest() || user.getPasswordHash() == null
                || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        return issueTokens(user);
    }

    /**
     * Creates a randomly named guest without email, password or stored profile language. Issues the
     * tokens needed to participate in games.
     */
    @Transactional
    public AuthResponse guestLogin() {
        String username = "guest_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        User user = User.builder()
                .username(username)
                .email(null)
                .passwordHash(null)
                .guest(true)
                .preferredLocale(null)
                .build();
        userRepository.save(user);
        return issueTokens(user);
    }

    /**
     * Validates the stored refresh token, revokes the submitted value and issues a replacement
     * pair. Rotation prevents normal reuse of the old refresh value.
     */
    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken stored = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

        if (stored.isRevoked() || stored.isExpired()) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // A new refresh token replaces the submitted one within the same transaction.
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);
        return issueTokens(stored.getUser());
    }

    /**
     * Revokes the matching refresh token when present. An already issued access token remains
     * subject to its own expiry.
     */
    @Transactional
    public void logout(RefreshTokenRequest request) {
        refreshTokenRepository.findByToken(request.getRefreshToken()).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    /**
     * Loads the account identified by the validated security principal and returns a safe
     * UserResponse.
     */
    @Transactional(readOnly = true)
    public UserResponse currentUser() {
        SecurityUser principal = CustomUserDetailsService.requireCurrent();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
        return DtoMapper.toUserResponse(user);
    }

    /**
     * Creates an access JWT and random refresh token, persists the refresh record and builds the
     * authentication response. Does not include password data.
     */
    private AuthResponse issueTokens(User user) {
        String access = jwtService.createAccessToken(user);
        String refreshValue = jwtService.createRefreshTokenValue();
        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshValue)
                .user(user)
                .expiresAt(jwtService.refreshExpiry())
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .user(DtoMapper.toUserResponse(user))
                .accessToken(access)
                .refreshToken(refreshValue)
                .tokenType("Bearer")
                .build();
    }
}
