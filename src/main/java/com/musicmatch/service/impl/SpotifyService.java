package com.musicmatch.service.impl;

import com.musicmatch.entity.Song;
import com.musicmatch.exception.SpotifyApiException;
import com.musicmatch.repository.SongRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.*;

@Service
@Slf4j
public class SpotifyService {

    private final SongRepository songRepository;
    private final RestTemplate restTemplate;

    @Value("${spotify.client-id}")
    private String clientId;

    @Value("${spotify.client-secret}")
    private String clientSecret;

    @Value("${spotify.token-url}")
    private String tokenUrl;

    @Value("${spotify.api-url}")
    private String apiUrl;

    // Simple in-memory token cache
    private String cachedToken;
    private Instant tokenExpiresAt = Instant.EPOCH;

    public SpotifyService(SongRepository songRepository) {
        this.songRepository = songRepository;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Search Spotify for tracks matching the query and persist new ones to DB.
     * Returns a list of songs (existing + newly created).
     */
    public List<Song> searchAndSave(String query, int limit) {
        String token = getAccessToken();
        String url = apiUrl + "/search?q=" + encodeQuery(query) + "&type=track&limit=" + limit;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            if (response.getBody() == null) {
                throw new SpotifyApiException("Empty response from Spotify API");
            }

            List<Song> results = new ArrayList<>();
            Map<String, Object> tracks = (Map<String, Object>) response.getBody().get("tracks");
            List<Map<String, Object>> items = (List<Map<String, Object>>) tracks.get("items");

            for (Map<String, Object> item : items) {
                String spotifyId = (String) item.get("id");

                // Reuse existing song or create new one
                Song song = songRepository.findBySpotifyId(spotifyId)
                    .orElseGet(() -> buildSongFromItem(item, spotifyId));

                if (song.getId() == null) {
                    song = songRepository.save(song);
                    log.info("New song saved from Spotify: {} - {}", song.getArtist(), song.getTitle());
                }
                results.add(song);
            }
            return results;

        } catch (SpotifyApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Spotify search failed for query '{}': {}", query, e.getMessage());
            throw new SpotifyApiException("Spotify API call failed: " + e.getMessage());
        }
    }

    /**
     * Client Credentials Flow — fetches and caches the access token.
     */
    String getAccessToken() {
        if (cachedToken != null && Instant.now().isBefore(tokenExpiresAt)) {
            return cachedToken;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String credentials = java.util.Base64.getEncoder()
            .encodeToString((clientId + ":" + clientSecret).getBytes());
        headers.set("Authorization", "Basic " + credentials);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);
            if (response.getBody() == null) {
                throw new SpotifyApiException("Failed to obtain Spotify token");
            }

            cachedToken = (String) response.getBody().get("access_token");
            int expiresIn = (Integer) response.getBody().get("expires_in");
            tokenExpiresAt = Instant.now().plusSeconds(expiresIn - 60); // 1 min buffer

            log.info("Spotify access token refreshed, expires in {}s", expiresIn);
            return cachedToken;

        } catch (SpotifyApiException e) {
            throw e;
        } catch (Exception e) {
            throw new SpotifyApiException("Could not authenticate with Spotify: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Song buildSongFromItem(Map<String, Object> item, String spotifyId) {
        String title = (String) item.get("name");

        List<Map<String, Object>> artists = (List<Map<String, Object>>) item.get("artists");
        String artist = artists.isEmpty() ? "Unknown" : (String) artists.get(0).get("name");

        Map<String, Object> album = (Map<String, Object>) item.get("album");
        String albumName = album != null ? (String) album.get("name") : null;

        String coverUrl = null;
        if (album != null) {
            List<Map<String, Object>> images = (List<Map<String, Object>>) album.get("images");
            if (images != null && !images.isEmpty()) {
                coverUrl = (String) images.get(0).get("url");
            }
        }

        String previewUrl = (String) item.get("preview_url");
        Integer durationMs = (Integer) item.get("duration_ms");

        return Song.builder()
            .spotifyId(spotifyId)
            .title(title)
            .artist(artist)
            .albumName(albumName)
            .coverUrl(coverUrl)
            .previewUrl(previewUrl)
            .durationMs(durationMs)
            .build();
    }

    private String encodeQuery(String query) {
        try {
            return java.net.URLEncoder.encode(query, "UTF-8");
        } catch (Exception e) {
            return query;
        }
    }
}
