package com.musicmatch.api.music.service;

import com.musicmatch.song.domain.Song;
import com.musicmatch.exceptions.SpotifyApiException;
import com.musicmatch.song.repository.SongRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.ParameterizedTypeReference;

import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.Objects;

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
        String token = Objects.requireNonNull(getAccessToken());
        String url = apiUrl + "/search?q=" + encodeQuery(query) + "&type=track&limit=" + limit;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(Objects.requireNonNull(token));
        URI requestUri = Objects.requireNonNull(URI.create(url));
        RequestEntity<Void> request = RequestEntity.get(requestUri)
            .headers(headers)
            .build();

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                throw new SpotifyApiException("Empty response from Spotify API");
            }

            List<Song> results = new ArrayList<>();
            Map<String, Object> tracks = requireMap(responseBody.get("tracks"), "tracks");
            List<Map<String, Object>> items = requireMapList(tracks.get("items"), "items");

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

        try {
            RequestEntity<MultiValueMap<String, String>> tokenRequest = RequestEntity
                .post(Objects.requireNonNull(URI.create(tokenUrl)))
                .headers(headers)
                .body(body);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                tokenRequest,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                throw new SpotifyApiException("Failed to obtain Spotify token");
            }

            cachedToken = Objects.toString(responseBody.get("access_token"), null);
            Integer expiresInValue = requireInteger(responseBody.get("expires_in"), "expires_in");
            int expiresIn = expiresInValue;
            tokenExpiresAt = Instant.now().plusSeconds(expiresIn - 60); // 1 min buffer

            log.info("Spotify access token refreshed, expires in {}s", expiresIn);
            return cachedToken;

        } catch (SpotifyApiException e) {
            throw e;
        } catch (Exception e) {
            throw new SpotifyApiException("Could not authenticate with Spotify: " + e.getMessage());
        }
    }

    private Song buildSongFromItem(Map<String, Object> item, String spotifyId) {
        String title = (String) item.get("name");

        List<Map<String, Object>> artists = requireMapList(item.get("artists"), "artists");
        String artist = artists.isEmpty() ? "Unknown" : (String) artists.get(0).get("name");

        Map<String, Object> album = requireMap(item.get("album"), "album");
        String albumName = album != null ? (String) album.get("name") : null;

        String coverUrl = null;
        if (album != null) {
            List<Map<String, Object>> images = requireMapList(album.get("images"), "images");
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> requireMap(Object value, String fieldName) {
        if (value == null) {
            throw new SpotifyApiException("Missing " + fieldName + " in Spotify response");
        }
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> requireMapList(Object value, String fieldName) {
        if (value == null) {
            return Collections.emptyList();
        }
        return (List<Map<String, Object>>) value;
    }

    private Integer requireInteger(Object value, String fieldName) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        throw new SpotifyApiException("Missing or invalid " + fieldName + " in Spotify response");
    }
}
