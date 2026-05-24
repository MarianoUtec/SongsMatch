package com.musicmatch.recommendation.service;

import com.musicmatch.recommendation.dto.request.RatingRequest;
import com.musicmatch.recommendation.dto.response.RatingResponse;

import java.util.List;

public interface IRatingService {
    RatingResponse rate(RatingRequest request);
    List<RatingResponse> getMyRatings();
    void deleteRating(Long ratingId);
}
