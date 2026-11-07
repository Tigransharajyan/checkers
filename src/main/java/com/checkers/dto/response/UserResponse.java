package com.checkers.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Safe public account data: id, username, guest flag, language and result counters. Passwords and
 * hashes are excluded. Lombok generates accessors and a builder.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private Long id;
    private String username;
    private String email;
    private String preferredLocale;
    private boolean guest;
    private int wins;
    private int losses;
    private int draws;
}
