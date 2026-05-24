package com.musicmatch.user.dto.request;
import jakarta.validation.constraints.Size;
public record UpdateUserRequest(@Size(min = 2, max = 50) String name, String spotifyId) {}
