package com.musicmatch.dto.response;
import java.time.LocalDateTime;
public record RatingResponse(Long id, Long userId, SongResponse song, Integer score, LocalDateTime createdAt, LocalDateTime updatedAt) {}
