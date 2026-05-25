package com.musicmatch.auth.controller;

import com.musicmatch.auth.service.IAuthService;
import com.musicmatch.auth.service.RegistrationResult;

import io.swagger.v3.oas.annotations.Operation;
import com.musicmatch.auth.dto.request.LoginRequest;
import com.musicmatch.auth.dto.request.RegisterRequest;
import com.musicmatch.auth.dto.response.AuthResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final IAuthService authService;

    @PostMapping("/register")
    @Operation(security = {})
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegistrationResult result = authService.register(request);
        if (result.emailSent()) {
            return ResponseEntity.ok(result.authResponse());
        } else {
            return ResponseEntity.status(HttpStatus.CREATED).body(result.authResponse());
        }
    }

    @PostMapping("/login")
    @Operation(security = {})
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(security = {})
    public ResponseEntity<AuthResponse> refresh(@RequestParam String refreshToken) {
        return ResponseEntity.ok(authService.refreshToken(refreshToken));
    }
}
