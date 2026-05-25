package com.musicmatch.recommendation.controller;

import com.musicmatch.recommendation.dto.response.LatentProfileResponse;
import com.musicmatch.recommendation.dto.response.LatentSpaceResponse;
import com.musicmatch.recommendation.dto.response.RecommendationResponse;
import com.musicmatch.song.dto.response.SongResponse;
import com.musicmatch.exceptions.ResourceNotFoundException;
import com.musicmatch.config.SecurityConfig;
import com.musicmatch.config.jwt.JwtAuthenticationFilter;
import com.musicmatch.config.jwt.JwtService;
import com.musicmatch.recommendation.service.RecommendationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RecommendationController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
@DisplayName("RecommendationController Tests")
class RecommendationControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private RecommendationService recommendationService;
    @MockBean private JwtService jwtService;
    @MockBean private UserDetailsService userDetailsService;

    @Test
    @WithMockUser
    @DisplayName("shouldReturn200WithRecommendationsWhenUserHasProfile")
    void shouldReturn200WithRecommendationsWhenUserHasProfile() throws Exception {
        SongResponse song = new SongResponse(1L, "Song A", "Artist A",
            null, null, null, null, null, null, null);
        RecommendationResponse response = new RecommendationResponse(
            1L, 1L, List.of(song), 2L, "Bob", LocalDateTime.now());

        when(recommendationService.getMyRecommendations()).thenReturn(response);

        mockMvc.perform(get("/api/v1/recommendations/me"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(1))
            .andExpect(jsonPath("$.songs[0].title").value("Song A"))
            .andExpect(jsonPath("$.basedOnUserName").value("Bob"));
    }

    @Test
    @WithMockUser
    @DisplayName("shouldReturn404WhenNoRecommendationsYet")
    void shouldReturn404WhenNoRecommendationsYet() throws Exception {
        when(recommendationService.getMyRecommendations())
            .thenThrow(new ResourceNotFoundException("No recommendations yet"));

        mockMvc.perform(get("/api/v1/recommendations/me"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    @DisplayName("shouldReturn200WithLatentProfileWhenUserHasProfile")
    void shouldReturn200WithLatentProfileWhenUserHasProfile() throws Exception {
        LatentProfileResponse profile = new LatentProfileResponse(
            1L, 0.5, 0.3, 0.2, 2L, 87.0, LocalDateTime.now());

        when(recommendationService.getMyLatentProfile()).thenReturn(profile);

        mockMvc.perform(get("/api/v1/users/me/latent-profile"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(1))
            .andExpect(jsonPath("$.compatibilityScore").value(87.0));
    }

    @Test
    @WithMockUser
    @DisplayName("shouldReturn200WithLatentSpaceWhenGetAllProfiles")
    void shouldReturn200WithLatentSpaceWhenGetAllProfiles() throws Exception {
        LatentSpaceResponse.UserLatentPoint point = new LatentSpaceResponse.UserLatentPoint(
            1L, "Alice", 0.5, 0.3, 0.2, 87.0, 2L);
        LatentSpaceResponse response = new LatentSpaceResponse(List.of(point));

        when(recommendationService.getLatentSpace()).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/latent-space"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.users[0].userName").value("Alice"))
            .andExpect(jsonPath("$.users[0].x").value(0.5));
    }

    @Test
    @DisplayName("shouldReturn401WhenGetRecommendationsWithoutAuth")
    void shouldReturn401WhenGetRecommendationsWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/v1/recommendations/me"))
            .andExpect(status().isUnauthorized());
    }
}
