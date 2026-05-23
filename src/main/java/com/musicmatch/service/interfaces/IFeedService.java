package com.musicmatch.service.interfaces;

import com.musicmatch.dto.response.FeedItemResponse;

import java.util.List;

public interface IFeedService {
    List<FeedItemResponse> getMyFeed();
}
