package com.musicmatch.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.musicmatch.auth.dto.request.LoginRequest;
import com.musicmatch.auth.dto.request.RegisterRequest;
import com.musicmatch.auth.dto.response.AuthResponse;
import com.musicmatch.user.dto.response.UserResponse;
import com.musicmatch.auth.domain.Role;
import com.musicmatch.exceptions.DuplicateResourceException;
import com.musicmatch.exceptions.UnauthorizedException;
import com.musicmatch.config.SecurityConfig;
import com.musicmatch.config.jwt.JwtAuthenticationFilter;
import com.musicmatch.config.jwt.JwtService;
import com.musicmatch.auth.service.AuthService;
import com.musicmatch.auth.service.RegistrationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
@DisplayName("AuthController Tests")
@SuppressWarnings("null")
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AuthService authService;
    @MockBean private JwtService jwtService;
    @MockBean private UserDetailsService userDetailsService;

    private AuthResponse mockAuthResponse;

    @BeforeEach
    void setUp() {
        UserResponse userResponse = new UserResponse(1L, "Alice", "alice@test.com",
            Role.USER, true, null, LocalDateTime.now());
        mockAuthResponse = new AuthResponse("access_token", "refresh_token", "Bearer", userResponse);
    }

    @Test
    @DisplayName("shouldReturn200WhenRegisterWithValidDataAndEmailSent")
    void shouldReturn201WhenRegisterWithValidData() throws Exception {
        RegisterRequest request = new RegisterRequest("Alice", "alice@test.com", "Password1");
        when(authService.register(any(RegisterRequest.class))).thenReturn(new RegistrationResult(mockAuthResponse, true));

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("access_token"))
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.user.email").value("alice@test.com"));
    }

    @Test
    @DisplayName("shouldReturn400WhenRegisterWithInvalidEmail")
    void shouldReturn400WhenRegisterWithInvalidEmail() throws Exception {
        RegisterRequest request = new RegisterRequest("Alice", "not-an-email", "Password1");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("shouldReturn400WhenRegisterWithShortPassword")
    void shouldReturn400WhenRegisterWithShortPassword() throws Exception {
        RegisterRequest request = new RegisterRequest("Alice", "alice@test.com", "abc");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("shouldReturn409WhenRegisterWithDuplicateEmail")
    void shouldReturn409WhenRegisterWithDuplicateEmail() throws Exception {
        RegisterRequest request = new RegisterRequest("Alice", "alice@test.com", "Password1");
        when(authService.register(any())).thenThrow(new DuplicateResourceException("Email already registered"));

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("shouldReturn200WhenLoginWithValidCredentials")
    void shouldReturn200WhenLoginWithValidCredentials() throws Exception {
        LoginRequest request = new LoginRequest("alice@test.com", "Password1");
        when(authService.login(any(LoginRequest.class))).thenReturn(mockAuthResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("access_token"))
            .andExpect(jsonPath("$.user.name").value("Alice"));
    }

    @Test
    @DisplayName("shouldReturn401WhenLoginWithInvalidCredentials")
    void shouldReturn401WhenLoginWithInvalidCredentials() throws Exception {
        LoginRequest request = new LoginRequest("alice@test.com", "wrong");
        when(authService.login(any())).thenThrow(new UnauthorizedException("Invalid credentials"));

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("shouldReturn200WhenRefreshTokenIsValid")
    void shouldReturn200WhenRefreshTokenIsValid() throws Exception {
        when(authService.refreshToken("valid_token")).thenReturn(mockAuthResponse);

        mockMvc.perform(post("/api/v1/auth/refresh")
                .param("refreshToken", "valid_token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("access_token"));
    }
}
