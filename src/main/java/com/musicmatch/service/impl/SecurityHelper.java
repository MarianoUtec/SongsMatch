package com.musicmatch.service.impl;

import com.musicmatch.entity.User;
import com.musicmatch.exception.ResourceNotFoundException;
import com.musicmatch.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Shared component to get the authenticated user from SecurityContext.
 * Avoids duplicating this logic in every service.
 */
@Component
@RequiredArgsConstructor
public class SecurityHelper {

    private final UserRepository userRepository;

    public User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }

    public Long getCurrentUserId() {
        return getCurrentUser().getId();
    }
}
