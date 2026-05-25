package com.musicmatch.api.music.service;

import com.musicmatch.song.domain.Song;
import com.musicmatch.song.repository.SongRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@Slf4j
public class MusicbrainzService {

    private final SongRepository songRepository;
    private final SpotifyService spotifyService;
    private final RestTemplate restTemplate;

    @Value("${musicbrainz.api-url:https://musicbrainz.org/ws/2}")
    private String apiUrl;

    public MusicbrainzService(SongRepository songRepository, SpotifyService spotifyService) {
        this.songRepository = songRepository;
        this.spotifyService = spotifyService;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Search MusicBrainz for tracks matching the query, persist them,
     * and fallback to Spotify if MusicBrainz fails or returns no results.
     */
    public List<Song> searchAndSave(String query, int limit) {
        try {
            log.info("Searching MusicBrainz for query: {}", query);
            List<Song> results = searchMusicbrainzAndSave(query, limit);
            if (results.isEmpty()) {
                log.info("MusicBrainz returned 0 results for '{}'. Falling back to Spotify...", query);
                return spotifyService.searchAndSave(query, limit);
            }
            return results;
        } catch (Exception e) {
            log.warn("MusicBrainz search failed for query '{}': {}. Falling back to Spotify...", query, e.getMessage());
            return spotifyService.searchAndSave(query, limit);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Song> searchMusicbrainzAndSave(String query, int limit) {
        // Construct Lucene query or simple query search.
        // For general terms, Lucene query without prefix searches all fields.
        String encodedQuery = encodeQuery(query);
        String url = apiUrl + "/recording?query=" + encodedQuery + "&limit=" + limit + "&fmt=json";

        HttpHeaders headers = new HttpHeaders();
        // MusicBrainz requires a descriptive User-Agent
        headers.set("User-Agent", "MusicMatch/1.0 (contact@musicmatch.com)");
        headers.set("Accept", "application/json");

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

        if (response.getBody() == null) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> recordings = (List<Map<String, Object>>) response.getBody().get("recordings");
        if (recordings == null || recordings.isEmpty()) {
            return Collections.emptyList();
        }

        List<Song> results = new ArrayList<>();
        for (Map<String, Object> recording : recordings) {
            String mbid = (String) recording.get("id");
            if (mbid == null) continue;

            // Reuse existing song by musicbrainzId
            Song song = songRepository.findByMusicbrainzId(mbid)
                .orElseGet(() -> buildSongFromRecording(recording, mbid));

            if (song.getId() == null) {
                song = songRepository.save(song);
                log.info("New song saved from MusicBrainz: {} - {}", song.getArtist(), song.getTitle());
            }
            results.add(song);
        }

        return results;
    }

    @SuppressWarnings("unchecked")
    private Song buildSongFromRecording(Map<String, Object> recording, String mbid) {
        String title = (String) recording.get("title");

        // Parse artist
        List<Map<String, Object>> artistCredits = (List<Map<String, Object>>) recording.get("artist-credit");
        String artistName = "Unknown";
        if (artistCredits != null && !artistCredits.isEmpty()) {
            Map<String, Object> credit = artistCredits.get(0);
            Map<String, Object> artistObj = (Map<String, Object>) credit.get("artist");
            if (artistObj != null) {
                artistName = (String) artistObj.get("name");
            } else {
                artistName = (String) credit.get("name");
            }
        }

        // Parse album name
        List<Map<String, Object>> releases = (List<Map<String, Object>>) recording.get("releases");
        String albumName = null;
        String coverUrl = null;
        if (releases != null && !releases.isEmpty()) {
            Map<String, Object> release = releases.get(0);
            albumName = (String) release.get("title");
            String releaseId = (String) release.get("id");
            if (releaseId != null) {
                // Point to Cover Art Archive front cover
                coverUrl = "https://coverartarchive.org/release/" + releaseId + "/front";
            }
        }

        // Parse length / duration
        Integer durationMs = null;
        Object lengthObj = recording.get("length");
        if (lengthObj instanceof Number) {
            durationMs = ((Number) lengthObj).intValue();
        }

        return Song.builder()
            .musicbrainzId(mbid)
            .title(title != null ? title : "Unknown")
            .artist(artistName != null ? artistName : "Unknown")
            .albumName(albumName)
            .coverUrl(coverUrl)
            .durationMs(durationMs)
            .build();
    }

    private String encodeQuery(String query) {
        try {
            // Replace spaces with Lucene AND operator or just URL-encode
            // Standard Lucene format for matching both artist and recording or simple search
            return java.net.URLEncoder.encode(query, "UTF-8");
        } catch (Exception e) {
            return query;
        }
    }
}
