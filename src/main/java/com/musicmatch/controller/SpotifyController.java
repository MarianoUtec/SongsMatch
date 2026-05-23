package com.musicmatch.controller;

import com.musicmatch.dto.response.SongResponse;
import com.musicmatch.mapper.SongMapper;
import com.musicmatch.service.impl.SpotifyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/spotify")
@RequiredArgsConstructor
public class SpotifyController {

    private final SpotifyService spotifyService;
    private final SongMapper songMapper;

    /**
     * Search Spotify for tracks matching a query, persist new ones to DB,
     * and return the combined list as SongResponse.
     *
     * GET /api/v1/spotify/search?q=queen&limit=10
     */
    @GetMapping("/search")
    public ResponseEntity<List<SongResponse>> search(
        @RequestParam String q,
        @RequestParam(defaultValue = "10") int limit
    ) {
        List<SongResponse> songs = spotifyService.searchAndSave(q, Math.min(limit, 50))
            .stream().map(songMapper::toResponse).toList();
        return ResponseEntity.ok(songs);
    }
}
