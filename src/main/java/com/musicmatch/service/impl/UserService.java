package com.musicmatch.service.impl;

import com.musicmatch.dto.request.UpdateUserRequest;
import com.musicmatch.dto.response.UserResponse;
import com.musicmatch.entity.User;
import com.musicmatch.exception.ResourceNotFoundException;
import com.musicmatch.mapper.UserMapper;
import com.musicmatch.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService implements com.musicmatch.service.interfaces.IUserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserResponse getMyProfile() {
        return userMapper.toResponse(getCurrentUser());
    }

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

    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(userMapper::toResponse);
    }

    @Transactional
    public void deactivateUser(Long userId) {
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
