package com.checkers.security;

import com.checkers.exception.BusinessException;
import com.checkers.exception.ErrorCode;
import com.checkers.exception.ResourceNotFoundException;
import com.checkers.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Connects persisted users to Spring Security and exposes the authenticated request principal.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Loads an account by username or email and converts the database entity to Spring Security's
     * user representation.
     */
    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        return userRepository.findByIdentifier(usernameOrEmail.trim())
                .map(SecurityUser::new)
                .orElseThrow(() -> new UsernameNotFoundException(usernameOrEmail));
    }

    /**
     * Loads the user identified by a token. Raises USER_NOT_FOUND if the account no longer exists.
     */
    public SecurityUser loadById(Long id) {
        return userRepository.findById(id)
                .map(SecurityUser::new)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * Extracts SecurityUser from the current thread's security context. Rejects an anonymous or
     * incompatible principal as UNAUTHORIZED.
     */
    public static SecurityUser requireCurrent() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof SecurityUser user)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return user;
    }

    /**
     * Returns the authenticated user's identifier through requireCurrent rather than trusting a
     * user id supplied by the client.
     */
    public static Long requireCurrentUserId() {
        return requireCurrent().getId();
    }
}
