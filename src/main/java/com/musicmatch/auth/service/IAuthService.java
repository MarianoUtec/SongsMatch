package com.musicmatch.auth.service;

import com.musicmatch.auth.dto.request.LoginRequest;
import com.musicmatch.auth.dto.request.RegisterRequest;
import com.musicmatch.auth.dto.response.AuthResponse;

public interface IAuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refreshToken(String refreshToken);
}
