package com.musicmatch.api.music.controller;

import com.musicmatch.song.dto.response.SongResponse;
import com.musicmatch.song.domain.Song;
import com.musicmatch.exceptions.SpotifyApiException;
import com.musicmatch.song.mapper.SongMapper;
import com.musicmatch.config.jwt.JwtAuthenticationFilter;
import com.musicmatch.config.jwt.JwtService;
import com.musicmatch.api.music.service.SpotifyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SpotifyController.class)
@DisplayName("SpotifyController Tests")
class SpotifyControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private SpotifyService spotifyService;
    @MockBean private SongMapper songMapper;
    @MockBean private JwtService jwtService;
    @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean private UserDetailsService userDetailsService;

    private Song mockSong;
    private SongResponse mockSongResponse;

    @BeforeEach
    void setUp() {
        mockSong = Song.builder().id(1L).title("Bohemian Rhapsody")
            .artist("Queen").spotifyId("sp_bohemian").build();
        mockSongResponse = new SongResponse(1L, "Bohemian Rhapsody", "Queen",
            null, null, "sp_bohemian", null, null, null, null);
    }

    @Test
    @WithMockUser
    @DisplayName("shouldReturn200WithSongsWhenSearchWithValidQuery")
    void shouldReturn200WithSongsWhenSearchWithValidQuery() throws Exception {
        when(spotifyService.searchAndSave(eq("queen"), anyInt()))
            .thenReturn(List.of(mockSong));
        when(songMapper.toResponse(mockSong)).thenReturn(mockSongResponse);

        mockMvc.perform(get("/api/v1/spotify/search")
                .param("q", "queen"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].title").value("Bohemian Rhapsody"))
            .andExpect(jsonPath("$[0].artist").value("Queen"))
            .andExpect(jsonPath("$[0].spotifyId").value("sp_bohemian"));
    }

    @Test
    @WithMockUser
    @DisplayName("shouldReturn200WithEmptyListWhenSearchReturnsNoResults")
    void shouldReturn200WithEmptyListWhenSearchReturnsNoResults() throws Exception {
        when(spotifyService.searchAndSave(eq("xyzunknownband123"), anyInt()))
            .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/spotify/search")
                .param("q", "xyzunknownband123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @WithMockUser
    @DisplayName("shouldReturn200WithMultipleSongsWhenSearchMatchesMultiple")
    void shouldReturn200WithMultipleSongsWhenSearchMatchesMultiple() throws Exception {
        Song song2 = Song.builder().id(2L).title("We Will Rock You")
            .artist("Queen").spotifyId("sp_wwry").build();
        SongResponse response2 = new SongResponse(2L, "We Will Rock You", "Queen",
            null, null, "sp_wwry", null, null, null, null);

        when(spotifyService.searchAndSave(eq("queen"), anyInt()))
            .thenReturn(List.of(mockSong, song2));
        when(songMapper.toResponse(mockSong)).thenReturn(mockSongResponse);
        when(songMapper.toResponse(song2)).thenReturn(response2);

        mockMvc.perform(get("/api/v1/spotify/search")
                .param("q", "queen")
                .param("limit", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[1].title").value("We Will Rock You"));
    }

    @Test
    @WithMockUser
    @DisplayName("shouldReturn502WhenSpotifyApiIsUnavailable")
    void shouldReturn502WhenSpotifyApiIsUnavailable() throws Exception {
        when(spotifyService.searchAndSave(any(), anyInt()))
            .thenThrow(new SpotifyApiException("Spotify API unavailable"));

        mockMvc.perform(get("/api/v1/spotify/search")
                .param("q", "queen"))
            .andExpect(status().isBadGateway());
    }

    @Test
    @DisplayName("shouldReturn401WhenSearchSpotifyWithoutAuthentication")
    void shouldReturn401WhenSearchSpotifyWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/spotify/search")
                .param("q", "queen"))
            .andExpect(status().isUnauthorized());
    }
}
