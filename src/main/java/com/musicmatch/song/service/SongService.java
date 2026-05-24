package com.musicmatch.song.service;

import com.musicmatch.song.dto.response.SongResponse;
import com.musicmatch.song.domain.Song;
import com.musicmatch.exceptions.ResourceNotFoundException;
import com.musicmatch.song.mapper.SongMapper;
import com.musicmatch.song.repository.SongRepository;
import com.musicmatch.song.service.ISongService;
import com.musicmatch.auth.service.SecurityHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SongService implements ISongService {

    private final SongRepository songRepository;
    private final SongMapper songMapper;
    private final SecurityHelper securityHelper;

    @Override
    public List<SongResponse> getAllSongs() {
        return songRepository.findAll().stream()
            .map(songMapper::toResponse).toList();
    }

    @Override
    public SongResponse getSongById(Long id) {
        Song song = songRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Song", id));
        return songMapper.toResponse(song);
    }

    @Override
    public List<SongResponse> search(String q) {
        List<Song> byTitle = songRepository.findByTitleContainingIgnoreCase(q);
        List<Long> foundIds = byTitle.stream().map(Song::getId).toList();
        List<Song> results = new ArrayList<>(byTitle);
        songRepository.findByArtistContainingIgnoreCase(q).stream()
            .filter(s -> !foundIds.contains(s.getId()))
            .forEach(results::add);
        return results.stream().map(songMapper::toResponse).toList();
    }

    @Override
    public List<SongResponse> getUnratedSongs() {
        Long userId = securityHelper.getCurrentUserId();
        return songRepository.findSongsNotRatedByUser(userId).stream()
            .map(songMapper::toResponse).toList();
    }

    @Override
    @Transactional
    public void deleteSong(Long id) {
        if (!songRepository.existsById(id))
            throw new ResourceNotFoundException("Song", id);
        songRepository.deleteById(id);
        log.info("Song {} deleted by admin", id);
    }
}
