package com.musicmatch.user.service;

import com.musicmatch.user.dto.request.UpdateUserRequest;
import com.musicmatch.user.dto.response.UserResponse;
import com.musicmatch.auth.domain.User;
import com.musicmatch.exceptions.ResourceNotFoundException;
import com.musicmatch.user.mapper.UserMapper;
import com.musicmatch.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService implements com.musicmatch.user.service.IUserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserResponse getMyProfile() {
        return userMapper.toResponse(getCurrentUser());
    }

    @SuppressWarnings("null")
    @Transactional
    public UserResponse updateMyProfile(UpdateUserRequest request) {
        User user = getCurrentUser();

        if (request.name() != null && !request.name().isBlank()) {
            user.setName(request.name());
        }
        if (request.spotifyId() != null) {
            user.setSpotifyId(request.spotifyId());
        }

        user = userRepository.save(user);
        log.info("User profile updated: {}", user.getEmail());
        return userMapper.toResponse(user);
    }

    public Page<UserResponse> getAllUsers(@Nullable Pageable pageable) {
        Pageable effectivePageable = pageable != null ? pageable : Pageable.unpaged();
        return userRepository.findAll(effectivePageable).map(userMapper::toResponse);
    }

    @Transactional
    public void deactivateUser(@Nullable Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        user.setIsActive(false);
        userRepository.save(user);
        log.info("User deactivated: {} (id={})", user.getEmail(), userId);
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }
}
