package com.musicmatch.dto.response;
public record SongResponse(Long id, String title, String artist, String albumName, String coverUrl, String spotifyId, String previewUrl, Double danceability, Double energy, Double valence) {}
