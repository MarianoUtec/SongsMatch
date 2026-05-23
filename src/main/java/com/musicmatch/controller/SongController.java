package com.musicmatch.controller;

import com.musicmatch.dto.response.SongResponse;
import com.musicmatch.service.interfaces.ISongService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/songs")
@RequiredArgsConstructor
public class SongController {

    private final ISongService songService;

    @GetMapping
    public ResponseEntity<List<SongResponse>> getAllSongs() {
        return ResponseEntity.ok(songService.getAllSongs());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SongResponse> getSong(@PathVariable Long id) {
        return ResponseEntity.ok(songService.getSongById(id));
    }

    @GetMapping("/search")
    public ResponseEntity<List<SongResponse>> search(@RequestParam String q) {
        return ResponseEntity.ok(songService.search(q));
    }

    @GetMapping("/unrated")
    public ResponseEntity<List<SongResponse>> getUnratedSongs() {
        return ResponseEntity.ok(songService.getUnratedSongs());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSong(@PathVariable Long id) {
        songService.deleteSong(id);
        return ResponseEntity.noContent().build();
    }
}
