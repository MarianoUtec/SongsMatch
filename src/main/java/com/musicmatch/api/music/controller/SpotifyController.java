package com.musicmatch.api.music.controller;

import com.musicmatch.song.dto.response.SongResponse;
import com.musicmatch.song.mapper.SongMapper;
import com.musicmatch.api.music.service.MusicbrainzService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/spotify")
@RequiredArgsConstructor
public class SpotifyController {

    private final MusicbrainzService musicbrainzService;
    private final SongMapper songMapper;

    /**
     * Search MusicBrainz (and fallback to Spotify) for tracks matching a query,
     * persist new ones to DB, and return the combined list as SongResponse.
     *
     * GET /api/v1/spotify/search?q=queen&limit=10
     */
    @GetMapping("/search")
    public ResponseEntity<List<SongResponse>> search(
        @RequestParam String q,
        @RequestParam(defaultValue = "10") int limit
    ) {
        List<SongResponse> songs = musicbrainzService.searchAndSave(q, Math.min(limit, 50))
            .stream().map(songMapper::toResponse).toList();
        return ResponseEntity.ok(songs);
    }
}
