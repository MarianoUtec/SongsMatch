package com.musicmatch.song.controller;

import com.musicmatch.song.dto.response.SongResponse;
import com.musicmatch.song.domain.Song;
import com.musicmatch.exceptions.ResourceNotFoundException;
import com.musicmatch.song.mapper.SongMapper;
import com.musicmatch.song.repository.SongRepository;
import com.musicmatch.config.jwt.JwtAuthenticationFilter;
import com.musicmatch.config.jwt.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SongController.class)
@DisplayName("SongController Tests")
class SongControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private SongRepository songRepository;
    @MockBean private SongMapper songMapper;
    @MockBean private JwtService jwtService;
    @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean private UserDetailsService userDetailsService;

    private Song mockSong;
    private SongResponse mockSongResponse;

    @BeforeEach
    void setUp() {
        mockSong = Song.builder().id(1L).title("Bohemian Rhapsody").artist("Queen")
            .spotifyId("sp1").build();
        mockSongResponse = new SongResponse(1L, "Bohemian Rhapsody", "Queen",
            null, null, "sp1", null, null, null, null);
    }

    @Test
    @DisplayName("shouldReturn200WithSongListWhenGetAllSongs")
    void shouldReturn200WithSongListWhenGetAllSongs() throws Exception {
        when(songRepository.findAll()).thenReturn(List.of(mockSong));
        when(songMapper.toResponse(mockSong)).thenReturn(mockSongResponse);

        mockMvc.perform(get("/api/v1/songs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].title").value("Bohemian Rhapsody"))
            .andExpect(jsonPath("$[0].artist").value("Queen"));
    }

    @Test
    @DisplayName("shouldReturn200WithSongWhenGetSongById")
    void shouldReturn200WithSongWhenGetSongById() throws Exception {
        when(songRepository.findById(1L)).thenReturn(Optional.of(mockSong));
        when(songMapper.toResponse(mockSong)).thenReturn(mockSongResponse);

        mockMvc.perform(get("/api/v1/songs/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.spotifyId").value("sp1"));
    }

    @Test
    @DisplayName("shouldReturn404WhenGetSongByNonExistingId")
    void shouldReturn404WhenGetSongByNonExistingId() throws Exception {
        when(songRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/songs/999"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("shouldReturn200WithResultsWhenSearchByQuery")
    void shouldReturn200WithResultsWhenSearchByQuery() throws Exception {
        when(songRepository.findByTitleContainingIgnoreCase("Queen")).thenReturn(List.of());
        when(songRepository.findByArtistContainingIgnoreCase("Queen")).thenReturn(List.of(mockSong));
        when(songMapper.toResponse(mockSong)).thenReturn(mockSongResponse);

        mockMvc.perform(get("/api/v1/songs/search").param("q", "Queen"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].artist").value("Queen"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("shouldReturn204WhenAdminDeletesSong")
    void shouldReturn204WhenAdminDeletesSong() throws Exception {
        mockMvc.perform(delete("/api/v1/songs/1")
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("shouldReturn403WhenUserTriesToDeleteSong")
    void shouldReturn403WhenUserTriesToDeleteSong() throws Exception {
        mockMvc.perform(delete("/api/v1/songs/1")
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
            .andExpect(status().isForbidden());
    }
}
