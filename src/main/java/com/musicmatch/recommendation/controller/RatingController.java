package com.musicmatch.recommendation.controller;

import com.musicmatch.recommendation.service.IRatingService;
import com.musicmatch.recommendation.dto.request.RatingRequest;
import com.musicmatch.recommendation.dto.response.RatingResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ratings")
@RequiredArgsConstructor
public class RatingController {

    private final IRatingService ratingService;

    @PostMapping
    public ResponseEntity<RatingResponse> rate(@Valid @RequestBody RatingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ratingService.rate(request));
    }

    @GetMapping("/me")
    public ResponseEntity<List<RatingResponse>> getMyRatings() {
        return ResponseEntity.ok(ratingService.getMyRatings());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRating(@PathVariable Long id) {
        ratingService.deleteRating(id);
        return ResponseEntity.noContent().build();
    }
}
