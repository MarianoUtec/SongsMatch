package com.musicmatch.dto.response;
import com.musicmatch.entity.Role;
import java.time.LocalDateTime;
public record UserResponse(Long id, String name, String email, Role role, Boolean isActive, String spotifyId, LocalDateTime createdAt) {}
