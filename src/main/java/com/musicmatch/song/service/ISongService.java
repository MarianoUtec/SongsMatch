package com.musicmatch.song.service;

import com.musicmatch.song.dto.response.SongResponse;
import org.springframework.lang.NonNull;

import java.util.List;

public interface ISongService {
    List<SongResponse> getAllSongs();
    SongResponse getSongById(@NonNull Long id);
    List<SongResponse> search(String q);
    List<SongResponse> getUnratedSongs();
    void deleteSong(@NonNull Long id);
}
