package com.musicmatch.service.interfaces;

import com.musicmatch.dto.response.SongResponse;

import java.util.List;

public interface ISongService {
    List<SongResponse> getAllSongs();
    SongResponse getSongById(Long id);
    List<SongResponse> search(String q);
    List<SongResponse> getUnratedSongs();
    void deleteSong(Long id);
}
