package com.checkers.service;

import com.checkers.dto.request.LocaleUpdateRequest;
import com.checkers.dto.response.UserResponse;
import com.checkers.exception.BusinessException;
import com.checkers.exception.ErrorCode;
import com.checkers.exception.ResourceNotFoundException;
import com.checkers.mapper.DtoMapper;
import com.checkers.model.entity.User;
import com.checkers.repository.UserRepository;
import com.checkers.security.CustomUserDetailsService;
import com.checkers.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Profile operations for safe account retrieval and supported registered-user language updates.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * Checks the supported language and permission to save profile preferences, then persists the
     * registered user's language.
     */
    @Transactional
    public UserResponse updatePreferredLocale(LocaleUpdateRequest request) {
        SecurityUser principal = CustomUserDetailsService.requireCurrent();
        if (principal.isGuest()) {
            throw new BusinessException(ErrorCode.GUEST_LOCALE_NOT_PERSISTED);
        }

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
        user.setPreferredLocale(request.getLocale());
        userRepository.save(user);
        return DtoMapper.toUserResponse(user);
    }

    /**
     * Loads a user by id and returns a safe public DTO.
     */
    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
        return DtoMapper.toUserResponse(user);
    }

    /**
     * Loads a user entity or raises the controlled USER_NOT_FOUND failure.
     */
    @Transactional(readOnly = true)
    public User requireEntity(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
    }
}
