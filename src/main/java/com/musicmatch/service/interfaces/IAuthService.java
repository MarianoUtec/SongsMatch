package com.musicmatch.service.interfaces;

import com.musicmatch.dto.request.LoginRequest;
import com.musicmatch.dto.request.RegisterRequest;
import com.musicmatch.dto.response.AuthResponse;

public interface IAuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refreshToken(String refreshToken);
}
