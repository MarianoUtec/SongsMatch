package com.musicmatch.song.dto.request;
import jakarta.validation.constraints.NotBlank;
public record SongCreateRequest(@NotBlank String title, @NotBlank String artist, String spotifyId, String albumName, String coverUrl) {}
