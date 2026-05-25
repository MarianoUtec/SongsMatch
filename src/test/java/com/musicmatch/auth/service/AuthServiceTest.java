package com.musicmatch.auth.service;

import com.musicmatch.auth.dto.request.LoginRequest;
import com.musicmatch.auth.dto.request.RegisterRequest;
import com.musicmatch.auth.dto.response.AuthResponse;
import com.musicmatch.user.dto.response.UserResponse;
import com.musicmatch.auth.domain.Role;
import com.musicmatch.auth.domain.User;
import com.musicmatch.exceptions.DuplicateResourceException;
import com.musicmatch.exceptions.UnauthorizedException;
import com.musicmatch.user.mapper.UserMapper;
import com.musicmatch.user.repository.UserRepository;
import com.musicmatch.config.jwt.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
@SuppressWarnings("null")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserDetailsService userDetailsService;
    @Mock private UserMapper userMapper;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AuthService authService;

    private User mockUser;
    private UserResponse mockUserResponse;
    private UserDetails mockUserDetails;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
            .id(1L).name("Alice").email("alice@test.com")
            .password("encoded_pw").role(Role.USER).isActive(true).build();

        mockUserResponse = new UserResponse(1L, "Alice", "alice@test.com",
            Role.USER, true, null, LocalDateTime.now());

        mockUserDetails = org.springframework.security.core.userdetails.User
            .withUsername("alice@test.com").password("encoded_pw").roles("USER").build();
    }

    @Test
    @DisplayName("shouldReturnAuthResponseWhenRegisterWithValidData")
    void shouldReturnAuthResponseWhenRegisterWithValidData() {
        RegisterRequest request = new RegisterRequest("Alice", "alice@test.com", "Password1");

        when(userRepository.existsByEmail("alice@test.com")).thenReturn(false);
        when(passwordEncoder.encode("Password1")).thenReturn("encoded_pw");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(userDetailsService.loadUserByUsername("alice@test.com")).thenReturn(mockUserDetails);
        when(jwtService.generateToken(mockUserDetails)).thenReturn("access_token");
        when(jwtService.generateRefreshToken(mockUserDetails)).thenReturn("refresh_token");
        when(userMapper.toResponse(mockUser)).thenReturn(mockUserResponse);

        RegistrationResult result = authService.register(request);
        AuthResponse response = result.authResponse();

        assertThat(response.accessToken()).isEqualTo("access_token");
        assertThat(response.refreshToken()).isEqualTo("refresh_token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.user().email()).isEqualTo("alice@test.com");
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    @DisplayName("shouldThrowDuplicateResourceExceptionWhenRegisterWithExistingEmail")
    void shouldThrowDuplicateResourceExceptionWhenRegisterWithExistingEmail() {
        RegisterRequest request = new RegisterRequest("Alice", "alice@test.com", "Password1");
        when(userRepository.existsByEmail("alice@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
            .isInstanceOf(DuplicateResourceException.class)
            .hasMessageContaining("alice@test.com");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("shouldReturnAuthResponseWhenLoginWithValidCredentials")
    void shouldReturnAuthResponseWhenLoginWithValidCredentials() {
        LoginRequest request = new LoginRequest("alice@test.com", "Password1");

        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(Objects.requireNonNull(mockUser)));
        when(userDetailsService.loadUserByUsername("alice@test.com")).thenReturn(mockUserDetails);
        when(jwtService.generateToken(mockUserDetails)).thenReturn("access_token");
        when(jwtService.generateRefreshToken(mockUserDetails)).thenReturn("refresh_token");
        when(userMapper.toResponse(mockUser)).thenReturn(mockUserResponse);

        AuthResponse response = authService.login(request);

        assertThat(response.accessToken()).isEqualTo("access_token");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("shouldThrowUnauthorizedExceptionWhenLoginWithBadCredentials")
    void shouldThrowUnauthorizedExceptionWhenLoginWithBadCredentials() {
        LoginRequest request = new LoginRequest("alice@test.com", "wrong_pw");
        doThrow(new BadCredentialsException("Bad credentials"))
            .when(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessageContaining("Invalid email or password");
    }

    @Test
    @DisplayName("shouldReturnNewAccessTokenWhenRefreshTokenIsValid")
    void shouldReturnNewAccessTokenWhenRefreshTokenIsValid() {
        String refreshToken = "valid_refresh_token";

        when(jwtService.extractUsername(refreshToken)).thenReturn("alice@test.com");
        when(userDetailsService.loadUserByUsername("alice@test.com")).thenReturn(mockUserDetails);
        when(jwtService.isTokenValid(refreshToken, mockUserDetails)).thenReturn(true);
        when(jwtService.generateToken(mockUserDetails)).thenReturn("new_access_token");
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(Objects.requireNonNull(mockUser)));
        when(userMapper.toResponse(mockUser)).thenReturn(mockUserResponse);

        AuthResponse response = authService.refreshToken(refreshToken);

        assertThat(response.accessToken()).isEqualTo("new_access_token");
        assertThat(response.refreshToken()).isEqualTo(refreshToken);
    }

    @Test
    @DisplayName("shouldThrowUnauthorizedExceptionWhenRefreshTokenIsInvalid")
    void shouldThrowUnauthorizedExceptionWhenRefreshTokenIsInvalid() {
        String expiredToken = "expired_token";

        when(jwtService.extractUsername(expiredToken)).thenReturn("alice@test.com");
        when(userDetailsService.loadUserByUsername("alice@test.com")).thenReturn(mockUserDetails);
        when(jwtService.isTokenValid(expiredToken, mockUserDetails)).thenReturn(false);

        assertThatThrownBy(() -> authService.refreshToken(expiredToken))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessageContaining("Invalid or expired refresh token");
    }
}
