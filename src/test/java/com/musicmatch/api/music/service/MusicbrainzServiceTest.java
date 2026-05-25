package com.musicmatch.api.music.service;

import com.musicmatch.song.domain.Song;
import com.musicmatch.song.repository.SongRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MusicbrainzService Tests")
@SuppressWarnings("null")
class MusicbrainzServiceTest {

    @Mock private SongRepository songRepository;
    @Mock private SpotifyService spotifyService;
    @Mock private RestTemplate restTemplate;

    private MusicbrainzService musicbrainzService;

    private Song mockSongSpotify;
    private Song mockSongMusicbrainz;

    @BeforeEach
    void setUp() {
        musicbrainzService = new MusicbrainzService(songRepository, spotifyService);
        // Inject mock restTemplate using Reflection
        ReflectionTestUtils.setField(musicbrainzService, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(musicbrainzService, "apiUrl", "https://musicbrainz.org/ws/2");

        mockSongSpotify = Song.builder().id(1L).title("Bohemian Rhapsody")
            .artist("Queen").spotifyId("sp_bohemian").build();

        mockSongMusicbrainz = Song.builder().id(2L).title("We Will Rock You")
            .artist("Queen").musicbrainzId("mb_wwry").build();
    }

    @Test
    @DisplayName("shouldReturnMusicbrainzSongsWhenMusicbrainzApiIsSuccessful")
    void shouldReturnMusicbrainzSongsWhenMusicbrainzApiIsSuccessful() {
        // Mock MusicBrainz Response
        Map<String, Object> responseBody = new HashMap<>();
        List<Map<String, Object>> recordings = new ArrayList<>();
        Map<String, Object> recording = new HashMap<>();
        recording.put("id", "mb_wwry");
        recording.put("title", "We Will Rock You");
        recording.put("length", 121000);

        Map<String, Object> artistCredit = new HashMap<>();
        Map<String, Object> artist = new HashMap<>();
        artist.put("name", "Queen");
        artistCredit.put("artist", artist);
        recording.put("artist-credit", List.of(artistCredit));

        Map<String, Object> release = new HashMap<>();
        release.put("title", "News of the World");
        release.put("id", "rel_123");
        recording.put("releases", List.of(release));

        recordings.add(recording);
        responseBody.put("recordings", recordings);

        ResponseEntity<Map<String, Object>> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.exchange(
            org.mockito.ArgumentMatchers.<RequestEntity<?>>any(),
            org.mockito.ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any()))
            .thenReturn(responseEntity);

        when(songRepository.findByMusicbrainzId("mb_wwry")).thenReturn(Optional.empty());
        when(songRepository.save(any(Song.class))).thenReturn(mockSongMusicbrainz);

        List<Song> results = musicbrainzService.searchAndSave("queen", 10);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("We Will Rock You");
        assertThat(results.get(0).getMusicbrainzId()).isEqualTo("mb_wwry");
        verify(songRepository).save(any(Song.class));
        verifyNoInteractions(spotifyService);
    }

    @Test
    @DisplayName("shouldFallbackToSpotifyWhenMusicbrainzApiReturnsEmptyList")
    void shouldFallbackToSpotifyWhenMusicbrainzApiReturnsEmptyList() {
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("recordings", Collections.emptyList());

        ResponseEntity<Map<String, Object>> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.exchange(
            org.mockito.ArgumentMatchers.<RequestEntity<?>>any(),
            org.mockito.ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any()))
            .thenReturn(responseEntity);

        when(spotifyService.searchAndSave("queen", 10)).thenReturn(List.of(mockSongSpotify));

        List<Song> results = musicbrainzService.searchAndSave("queen", 10);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("Bohemian Rhapsody");
        assertThat(results.get(0).getSpotifyId()).isEqualTo("sp_bohemian");
        verify(spotifyService).searchAndSave("queen", 10);
    }

    @Test
    @DisplayName("shouldFallbackToSpotifyWhenMusicbrainzApiThrowsException")
    void shouldFallbackToSpotifyWhenMusicbrainzApiThrowsException() {
        when(restTemplate.exchange(
            org.mockito.ArgumentMatchers.<RequestEntity<?>>any(),
            org.mockito.ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any()))
            .thenThrow(new RuntimeException("API is down"));

        when(spotifyService.searchAndSave("queen", 10)).thenReturn(List.of(mockSongSpotify));

        List<Song> results = musicbrainzService.searchAndSave("queen", 10);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("Bohemian Rhapsody");
        assertThat(results.get(0).getSpotifyId()).isEqualTo("sp_bohemian");
        verify(spotifyService).searchAndSave("queen", 10);
    }
}
