package com.musicmatch.recommendation.service;

import com.musicmatch.recommendation.dto.response.FeedItemResponse;

import java.util.List;

public interface IFeedService {
    List<FeedItemResponse> getMyFeed();
}
