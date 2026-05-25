package com.musicmatch.api.music.controller;

import com.musicmatch.song.dto.response.SongResponse;
import com.musicmatch.song.domain.Song;
import com.musicmatch.song.mapper.SongMapper;
import com.musicmatch.config.SecurityConfig;
import com.musicmatch.config.jwt.JwtAuthenticationFilter;
import com.musicmatch.config.jwt.JwtService;
import com.musicmatch.api.music.service.MusicbrainzService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MusicbrainzController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
@DisplayName("MusicbrainzController Tests")
class MusicbrainzControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private MusicbrainzService musicbrainzService;
    @MockBean private SongMapper songMapper;
    @MockBean private JwtService jwtService;
    @MockBean private UserDetailsService userDetailsService;

    private Song mockSong;
    private SongResponse mockSongResponse;

    @BeforeEach
    void setUp() {
        mockSong = Song.builder().id(1L).title("Bohemian Rhapsody")
            .artist("Queen").musicbrainzId("mb_bohemian").build();
        mockSongResponse = new SongResponse(1L, "Bohemian Rhapsody", "Queen",
            null, null, null, null, null, null, null, "mb_bohemian");
    }

    @Test
    @WithMockUser
    @DisplayName("shouldReturn200WithSongsWhenSearchWithValidQuery")
    void shouldReturn200WithSongsWhenSearchWithValidQuery() throws Exception {
        when(musicbrainzService.searchAndSave(eq("queen"), anyInt()))
            .thenReturn(List.of(mockSong));
        when(songMapper.toResponse(mockSong)).thenReturn(mockSongResponse);

        mockMvc.perform(get("/api/v1/musicbrainz/search")
                .param("q", "queen"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].title").value("Bohemian Rhapsody"))
            .andExpect(jsonPath("$[0].artist").value("Queen"))
            .andExpect(jsonPath("$[0].musicbrainzId").value("mb_bohemian"));
    }

    @Test
    @WithMockUser
    @DisplayName("shouldReturn200WithEmptyListWhenSearchReturnsNoResults")
    void shouldReturn200WithEmptyListWhenSearchReturnsNoResults() throws Exception {
        when(musicbrainzService.searchAndSave(eq("xyzunknownband123"), anyInt()))
            .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/musicbrainz/search")
                .param("q", "xyzunknownband123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("shouldReturn401WhenSearchWithoutAuthentication")
    void shouldReturn401WhenSearchWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/musicbrainz/search")
                .param("q", "queen"))
            .andExpect(status().isUnauthorized());
    }
}
