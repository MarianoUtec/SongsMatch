package com.musicmatch.user.dto.response;
import com.musicmatch.auth.domain.Role;
import java.time.LocalDateTime;
public record UserResponse(Long id, String name, String email, Role role, Boolean isActive, String spotifyId, LocalDateTime createdAt) {}
