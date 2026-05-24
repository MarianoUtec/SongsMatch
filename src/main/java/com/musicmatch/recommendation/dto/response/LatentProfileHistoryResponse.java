package com.musicmatch.recommendation.dto.response;
import java.time.LocalDateTime;
public record LatentProfileHistoryResponse(
    Long id,
    Double coordX,
    Double coordY,
    Double coordZ,
    Long closestUserId,
    String closestUserName,
    Double compatibilityScore,
    Integer ratingsCount,
    LocalDateTime recordedAt
) {}
