package com.musicmatch.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.musicmatch.user.dto.request.UpdateUserRequest;
import com.musicmatch.user.dto.response.UserResponse;
import com.musicmatch.auth.domain.Role;
import com.musicmatch.exceptions.ResourceNotFoundException;
import com.musicmatch.config.SecurityConfig;
import com.musicmatch.config.jwt.JwtAuthenticationFilter;
import com.musicmatch.config.jwt.JwtService;
import com.musicmatch.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
@DisplayName("UserController Tests")
class UserControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private UserService userService;
    @MockBean private JwtService jwtService;
    @MockBean private UserDetailsService userDetailsService;

    private UserResponse mockUserResponse;

    @BeforeEach
    void setUp() {
        mockUserResponse = new UserResponse(1L, "Alice", "alice@test.com",
            Role.USER, true, null, LocalDateTime.now());
    }

    @Test
    @WithMockUser
    @DisplayName("shouldReturn200WithProfileWhenGetMyProfile")
    void shouldReturn200WithProfileWhenGetMyProfile() throws Exception {
        when(userService.getMyProfile()).thenReturn(mockUserResponse);

        mockMvc.perform(get("/api/v1/users/me"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Alice"))
            .andExpect(jsonPath("$.email").value("alice@test.com"));
    }

    @Test
    @WithMockUser
    @DisplayName("shouldReturn200WithUpdatedProfileWhenUpdateMyProfile")
    void shouldReturn200WithUpdatedProfileWhenUpdateMyProfile() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest("Alice Updated", null);
        UserResponse updated = new UserResponse(1L, "Alice Updated", "alice@test.com",
            Role.USER, true, null, LocalDateTime.now());

        when(userService.updateMyProfile(any(UpdateUserRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/v1/users/me")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Alice Updated"));
    }

    @Test
    @DisplayName("shouldReturn401WhenGetMyProfileWithoutAuth")
    void shouldReturn401WhenGetMyProfileWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("shouldReturn200WithUserListWhenAdminGetAllUsers")
    void shouldReturn200WithUserListWhenAdminGetAllUsers() throws Exception {
        when(userService.getAllUsers(any())).thenReturn(
            new org.springframework.data.domain.PageImpl<>(List.of(mockUserResponse)));

        mockMvc.perform(get("/api/v1/admin/users"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("shouldReturn403WhenNonAdminTriesToListAllUsers")
    void shouldReturn403WhenNonAdminTriesToListAllUsers() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("shouldReturn204WhenAdminDeactivatesUser")
    void shouldReturn204WhenAdminDeactivatesUser() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/users/1").with(csrf()))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("shouldReturn404WhenAdminDeactivatesNonExistingUser")
    void shouldReturn404WhenAdminDeactivatesNonExistingUser() throws Exception {
        doThrow(new ResourceNotFoundException("User", 999L))
            .when(userService).deactivateUser(999L);

        mockMvc.perform(delete("/api/v1/admin/users/999").with(csrf()))
            .andExpect(status().isNotFound());
    }
}
