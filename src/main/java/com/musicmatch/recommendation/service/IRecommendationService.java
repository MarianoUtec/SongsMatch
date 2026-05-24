package com.musicmatch.recommendation.service;

import com.musicmatch.recommendation.dto.response.LatentProfileResponse;
import com.musicmatch.recommendation.dto.response.LatentSpaceResponse;
import com.musicmatch.recommendation.dto.response.RecommendationResponse;

public interface IRecommendationService {
    RecommendationResponse getMyRecommendations();
    LatentProfileResponse getMyLatentProfile();
    LatentSpaceResponse getLatentSpace();
}
