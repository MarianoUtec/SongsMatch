package com.musicmatch.song.controller;

import com.musicmatch.song.dto.response.SongResponse;
import com.musicmatch.exceptions.ResourceNotFoundException;
import com.musicmatch.song.service.ISongService;
import com.musicmatch.config.SecurityConfig;
import com.musicmatch.config.jwt.JwtAuthenticationFilter;
import com.musicmatch.config.jwt.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SongController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
@DisplayName("SongController Tests")
class SongControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private ISongService songService;
    @MockBean private JwtService jwtService;
    @MockBean private UserDetailsService userDetailsService;

    private SongResponse mockSongResponse;

    @BeforeEach
    void setUp() {
        mockSongResponse = new SongResponse(1L, "Bohemian Rhapsody", "Queen",
            null, null, "sp1", null, null, null, null);
    }

    @Test
    @WithMockUser
    @DisplayName("shouldReturn200WithSongListWhenGetAllSongs")
    void shouldReturn200WithSongListWhenGetAllSongs() throws Exception {
        when(songService.getAllSongs()).thenReturn(List.of(mockSongResponse));

        mockMvc.perform(get("/api/v1/songs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].title").value("Bohemian Rhapsody"))
            .andExpect(jsonPath("$[0].artist").value("Queen"));
    }

    @Test
    @WithMockUser
    @DisplayName("shouldReturn200WithSongWhenGetSongById")
    void shouldReturn200WithSongWhenGetSongById() throws Exception {
        when(songService.getSongById(1L)).thenReturn(mockSongResponse);

        mockMvc.perform(get("/api/v1/songs/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.spotifyId").value("sp1"));
    }

    @Test
    @WithMockUser
    @DisplayName("shouldReturn404WhenGetSongByNonExistingId")
    void shouldReturn404WhenGetSongByNonExistingId() throws Exception {
        when(songService.getSongById(999L)).thenThrow(new ResourceNotFoundException("Song", 999L));

        mockMvc.perform(get("/api/v1/songs/999"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    @DisplayName("shouldReturn200WithResultsWhenSearchByQuery")
    void shouldReturn200WithResultsWhenSearchByQuery() throws Exception {
        when(songService.search("Queen")).thenReturn(List.of(mockSongResponse));

        mockMvc.perform(get("/api/v1/songs/search").param("q", "Queen"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].artist").value("Queen"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("shouldReturn204WhenAdminDeletesSong")
    void shouldReturn204WhenAdminDeletesSong() throws Exception {
        doNothing().when(songService).deleteSong(1L);

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
