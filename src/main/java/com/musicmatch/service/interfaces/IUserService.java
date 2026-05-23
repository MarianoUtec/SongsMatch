package com.musicmatch.service.interfaces;

import com.musicmatch.dto.request.UpdateUserRequest;
import com.musicmatch.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IUserService {
    UserResponse getMyProfile();
    UserResponse updateMyProfile(UpdateUserRequest request);
    Page<UserResponse> getAllUsers(Pageable pageable);
    void deactivateUser(Long userId);
}
