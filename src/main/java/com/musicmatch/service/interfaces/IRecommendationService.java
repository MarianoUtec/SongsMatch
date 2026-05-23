package com.musicmatch.service.interfaces;

import com.musicmatch.dto.response.LatentProfileResponse;
import com.musicmatch.dto.response.LatentSpaceResponse;
import com.musicmatch.dto.response.RecommendationResponse;

public interface IRecommendationService {
    RecommendationResponse getMyRecommendations();
    LatentProfileResponse getMyLatentProfile();
    LatentSpaceResponse getLatentSpace();
}
