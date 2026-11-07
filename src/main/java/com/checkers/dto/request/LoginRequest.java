package com.checkers.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Incoming username/email and password DTO. The password is used for verification and never
 * included in the response. Lombok generates accessors.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {

    @NotBlank
    private String usernameOrEmail;

    @NotBlank
    private String password;
}
