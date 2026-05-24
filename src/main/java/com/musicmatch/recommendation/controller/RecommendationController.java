package com.musicmatch.recommendation.controller;

import com.musicmatch.recommendation.dto.response.LatentProfileResponse;
import com.musicmatch.recommendation.dto.response.LatentSpaceResponse;
import com.musicmatch.recommendation.dto.response.RecommendationResponse;
import com.musicmatch.recommendation.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping("/recommendations/me")
    public ResponseEntity<RecommendationResponse> getMyRecommendations() {
        return ResponseEntity.ok(recommendationService.getMyRecommendations());
    }

    @GetMapping("/users/me/latent-profile")
    public ResponseEntity<LatentProfileResponse> getMyLatentProfile() {
        return ResponseEntity.ok(recommendationService.getMyLatentProfile());
    }

    @GetMapping("/users/latent-space")
    public ResponseEntity<LatentSpaceResponse> getLatentSpace() {
        return ResponseEntity.ok(recommendationService.getLatentSpace());
    }
}
