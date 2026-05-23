package com.musicmatch.dto.response;
import java.time.LocalDateTime;
public record LatentProfileResponse(Long userId, Double coordX, Double coordY, Double coordZ, Long closestUserId, Double compatibilityScore, LocalDateTime updatedAt) {}
