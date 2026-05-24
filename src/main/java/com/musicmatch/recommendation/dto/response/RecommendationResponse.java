package com.musicmatch.recommendation.dto.response;

import com.musicmatch.song.dto.response.SongResponse;
import java.time.LocalDateTime;
import java.util.List;
public record RecommendationResponse(Long id, Long userId, List<SongResponse> songs, Long basedOnUserId, String basedOnUserName, LocalDateTime createdAt) {}
