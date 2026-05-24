package com.musicmatch.recommendation.dto.response;
import java.util.List;
public record LatentSpaceResponse(List<LatentSpaceResponse.UserLatentPoint> users) {
    public record UserLatentPoint(Long userId, String userName, Double x, Double y, Double z, Double compatibilityScore, Long closestUserId) {}
}
