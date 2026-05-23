package com.musicmatch.service.interfaces;

import com.musicmatch.dto.response.LatentProfileHistoryResponse;

import java.util.List;

public interface ILatentHistoryService {
    List<LatentProfileHistoryResponse> getMyHistory();
    List<LatentProfileHistoryResponse> getRecentHistory();
}
