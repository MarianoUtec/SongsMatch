package com.musicmatch.recommendation.dto.response;

import com.musicmatch.song.dto.response.SongResponse;
import java.time.LocalDateTime;
public record FeedItemResponse(
    Long userId,
    String userName,
    Double compatibilityScore,
    SongResponse song,
    Integer score,
    LocalDateTime ratedAt
) {}
