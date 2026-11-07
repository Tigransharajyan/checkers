package com.checkers.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Successful authentication response containing safe user data, an access JWT, a refresh token and
 * the Bearer scheme. Lombok generates accessors and a builder.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private UserResponse user;
    private String accessToken;
    private String refreshToken;
    private String tokenType;
}
