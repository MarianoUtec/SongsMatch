package com.musicmatch.recommendation.service;

import com.musicmatch.recommendation.dto.response.LatentProfileHistoryResponse;

import java.util.List;

public interface ILatentHistoryService {
    List<LatentProfileHistoryResponse> getMyHistory();
    List<LatentProfileHistoryResponse> getRecentHistory();
}
