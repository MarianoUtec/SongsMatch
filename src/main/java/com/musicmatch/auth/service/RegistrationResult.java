package com.musicmatch.auth.service;

import com.musicmatch.auth.dto.response.AuthResponse;

public record RegistrationResult(AuthResponse authResponse, boolean emailSent) {}
