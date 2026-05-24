package com.musicmatch.recommendation.controller;

import com.musicmatch.recommendation.service.IFeedService;

import com.musicmatch.recommendation.dto.response.FeedItemResponse;
import com.musicmatch.recommendation.service.FeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/feed")
@RequiredArgsConstructor
public class FeedController {

    private final IFeedService feedService;

    @GetMapping
    public ResponseEntity<List<FeedItemResponse>> getMyFeed() {
        return ResponseEntity.ok(feedService.getMyFeed());
    }
}
