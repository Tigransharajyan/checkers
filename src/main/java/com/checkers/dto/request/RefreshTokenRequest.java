package com.checkers.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Incoming opaque refresh-token DTO used to renew authentication or log out. Lombok generates
 * accessors.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshTokenRequest {

    @NotBlank
    private String refreshToken;
}
