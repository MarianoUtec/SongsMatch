package com.musicmatch.dto.response;
import java.time.LocalDateTime;
public record FeedItemResponse(
    Long userId,
    String userName,
    Double compatibilityScore,
    SongResponse song,
    Integer score,
    LocalDateTime ratedAt
) {}
