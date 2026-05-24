package com.musicmatch.auth.dto.response;

import com.musicmatch.user.dto.response.UserResponse;

public record AuthResponse(String accessToken, String refreshToken, String tokenType, UserResponse user) {
    public static AuthResponse of(String access, String refresh, UserResponse user) {
        return new AuthResponse(access, refresh, "Bearer", user);
    }
}
