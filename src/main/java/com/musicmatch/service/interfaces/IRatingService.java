package com.musicmatch.service.interfaces;

import com.musicmatch.dto.request.RatingRequest;
import com.musicmatch.dto.response.RatingResponse;

import java.util.List;

public interface IRatingService {
    RatingResponse rate(RatingRequest request);
    List<RatingResponse> getMyRatings();
    void deleteRating(Long ratingId);
}
