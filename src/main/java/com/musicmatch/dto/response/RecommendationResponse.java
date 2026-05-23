package com.musicmatch.dto.response;
import java.time.LocalDateTime;
import java.util.List;
public record RecommendationResponse(Long id, Long userId, List<SongResponse> songs, Long basedOnUserId, String basedOnUserName, LocalDateTime createdAt) {}
