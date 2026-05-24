package com.musicmatch.user.service;

import com.musicmatch.user.dto.request.UpdateUserRequest;
import com.musicmatch.user.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IUserService {
    UserResponse getMyProfile();
    UserResponse updateMyProfile(UpdateUserRequest request);
    Page<UserResponse> getAllUsers(Pageable pageable);
    void deactivateUser(Long userId);
}
