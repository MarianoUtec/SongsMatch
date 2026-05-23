package com.musicmatch.service;

import com.musicmatch.dto.request.UpdateUserRequest;
import com.musicmatch.dto.response.UserResponse;
import com.musicmatch.entity.Role;
import com.musicmatch.entity.User;
import com.musicmatch.exception.ResourceNotFoundException;
import com.musicmatch.mapper.UserMapper;
import com.musicmatch.repository.UserRepository;
import com.musicmatch.service.impl.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;
    @Mock private SecurityContext securityContext;
    @Mock private Authentication authentication;

    @InjectMocks
    private UserService userService;

    private User mockUser;
    private UserResponse mockUserResponse;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("alice@test.com");

        mockUser = User.builder()
            .id(1L).name("Alice").email("alice@test.com")
            .password("encoded_pw").role(Role.USER).isActive(true).build();

        mockUserResponse = new UserResponse(1L, "Alice", "alice@test.com",
            Role.USER, true, null, LocalDateTime.now());
    }

    // ─────────────────────────── getMyProfile ────────────────────────────

    @Test
    @DisplayName("shouldReturnUserResponseWhenGetMyProfileWithValidUser")
    void shouldReturnUserResponseWhenGetMyProfileWithValidUser() {
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(mockUser));
        when(userMapper.toResponse(mockUser)).thenReturn(mockUserResponse);

        UserResponse response = userService.getMyProfile();

        assertThat(response.email()).isEqualTo("alice@test.com");
        assertThat(response.name()).isEqualTo("Alice");
        verify(userMapper).toResponse(mockUser);
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundExceptionWhenGetMyProfileWithUnknownEmail")
    void shouldThrowResourceNotFoundExceptionWhenGetMyProfileWithUnknownEmail() {
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getMyProfile())
            .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─────────────────────────── updateMyProfile ─────────────────────────

    @Test
    @DisplayName("shouldUpdateNameWhenUpdateMyProfileWithNonBlankName")
    void shouldUpdateNameWhenUpdateMyProfileWithNonBlankName() {
        UpdateUserRequest request = new UpdateUserRequest("Bob", null);
        UserResponse updatedResponse = new UserResponse(1L, "Bob", "alice@test.com",
            Role.USER, true, null, LocalDateTime.now());

        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(mockUser));
        when(userRepository.save(mockUser)).thenReturn(mockUser);
        when(userMapper.toResponse(mockUser)).thenReturn(updatedResponse);

        UserResponse response = userService.updateMyProfile(request);

        assertThat(mockUser.getName()).isEqualTo("Bob");
        assertThat(response.name()).isEqualTo("Bob");
        verify(userRepository).save(mockUser);
    }

    @Test
    @DisplayName("shouldUpdateSpotifyIdWhenUpdateMyProfileWithSpotifyId")
    void shouldUpdateSpotifyIdWhenUpdateMyProfileWithSpotifyId() {
        UpdateUserRequest request = new UpdateUserRequest(null, "spotify123");

        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(mockUser));
        when(userRepository.save(mockUser)).thenReturn(mockUser);
        when(userMapper.toResponse(mockUser)).thenReturn(mockUserResponse);

        userService.updateMyProfile(request);

        assertThat(mockUser.getSpotifyId()).isEqualTo("spotify123");
    }

    @Test
    @DisplayName("shouldNotUpdateNameWhenUpdateMyProfileWithBlankName")
    void shouldNotUpdateNameWhenUpdateMyProfileWithBlankName() {
        UpdateUserRequest request = new UpdateUserRequest("   ", null);

        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(mockUser));
        when(userRepository.save(mockUser)).thenReturn(mockUser);
        when(userMapper.toResponse(mockUser)).thenReturn(mockUserResponse);

        userService.updateMyProfile(request);

        // name should remain unchanged when blank
        assertThat(mockUser.getName()).isEqualTo("Alice");
    }

    // ─────────────────────────── getAllUsers ──────────────────────────────

    @Test
    @DisplayName("shouldReturnPageOfUsersWhenGetAllUsers")
    void shouldReturnPageOfUsersWhenGetAllUsers() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(List.of(mockUser));

        when(userRepository.findAll(pageable)).thenReturn(userPage);
        when(userMapper.toResponse(mockUser)).thenReturn(mockUserResponse);

        Page<UserResponse> result = userService.getAllUsers(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).email()).isEqualTo("alice@test.com");
    }

    @Test
    @DisplayName("shouldReturnEmptyPageWhenGetAllUsersWithNoUsers")
    void shouldReturnEmptyPageWhenGetAllUsersWithNoUsers() {
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findAll(pageable)).thenReturn(Page.empty());

        Page<UserResponse> result = userService.getAllUsers(pageable);

        assertThat(result.getContent()).isEmpty();
    }

    // ─────────────────────────── deactivateUser ───────────────────────────

    @Test
    @DisplayName("shouldDeactivateUserWhenDeactivateUserWithValidId")
    void shouldDeactivateUserWhenDeactivateUserWithValidId() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(userRepository.save(mockUser)).thenReturn(mockUser);

        userService.deactivateUser(1L);

        assertThat(mockUser.getIsActive()).isFalse();
        verify(userRepository).save(mockUser);
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundExceptionWhenDeactivateUserWithInvalidId")
    void shouldThrowResourceNotFoundExceptionWhenDeactivateUserWithInvalidId() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deactivateUser(999L))
            .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository, never()).save(any());
    }
}
