package com.musicmatch.controller;

import com.musicmatch.service.interfaces.ILatentHistoryService;

import com.musicmatch.dto.response.LatentProfileHistoryResponse;
import com.musicmatch.service.impl.LatentHistoryService;
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
