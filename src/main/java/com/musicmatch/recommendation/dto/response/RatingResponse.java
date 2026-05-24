package com.musicmatch.recommendation.dto.response;

import com.musicmatch.song.dto.response.SongResponse;
import java.time.LocalDateTime;
public record RatingResponse(Long id, Long userId, SongResponse song, Integer score, LocalDateTime createdAt, LocalDateTime updatedAt) {}
