package com.checkers.security;

import com.checkers.model.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Spring Security account snapshot containing id, username, password hash, language and guest
 * state. Lombok generates accessors needed by UserDetails.
 */
@Getter
public class SecurityUser implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final String preferredLocale;
    private final boolean guest;

    /**
     * Creates a security snapshot of the account. Replaces a guest's missing password hash with an
     * empty string for the UserDetails contract.
     */
    public SecurityUser(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.password = user.getPasswordHash() == null ? "" : user.getPasswordHash();
        this.preferredLocale = user.getPreferredLocale();
        this.guest = user.isGuest();
    }

    /**
     * Assigns ROLE_GUEST to guests and ROLE_USER to registered users. These roles describe account
     * type, not a draughts color.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (guest) {
            return List.of(new SimpleGrantedAuthority("ROLE_GUEST"));
        }
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    /**
     * Returns true because the account model has no separate account-expiration mechanism.
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Returns true because this model has no independent account-lock state.
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * Returns true for UserDetails; JWT expiry is checked separately by token validation.
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Returns true because the current model has no disabled-account flag.
     */
    @Override
    public boolean isEnabled() {
        return true;
    }
}
