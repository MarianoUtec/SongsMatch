package com.musicmatch.recommendation.controller;

import com.musicmatch.recommendation.service.ILatentHistoryService;

import com.musicmatch.recommendation.dto.response.LatentProfileHistoryResponse;
import com.musicmatch.recommendation.service.LatentHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/me/twin-history")
@RequiredArgsConstructor
public class LatentHistoryController {

    private final ILatentHistoryService latentHistoryService;

    // Full history: all snapshots since first rating
    @GetMapping
    public ResponseEntity<List<LatentProfileHistoryResponse>> getFullHistory() {
        return ResponseEntity.ok(latentHistoryService.getMyHistory());
    }

    // Recent: last 10 snapshots
    @GetMapping("/recent")
    public ResponseEntity<List<LatentProfileHistoryResponse>> getRecentHistory() {
        return ResponseEntity.ok(latentHistoryService.getRecentHistory());
    }
}
