package com.musicmatch.recommendation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.musicmatch.recommendation.dto.request.RatingRequest;
import com.musicmatch.recommendation.dto.response.RatingResponse;
import com.musicmatch.song.dto.response.SongResponse;
import com.musicmatch.exceptions.ResourceNotFoundException;
import com.musicmatch.config.jwt.JwtAuthenticationFilter;
import com.musicmatch.config.jwt.JwtService;
import com.musicmatch.recommendation.service.RatingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RatingController.class)
@DisplayName("RatingController Tests")
class RatingControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private RatingService ratingService;
    @MockBean private JwtService jwtService;
    @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean private UserDetailsService userDetailsService;

    private RatingResponse mockRatingResponse;

    @BeforeEach
    void setUp() {
        SongResponse songResponse = new SongResponse(10L, "Song A", "Artist A",
            null, null, "sp1", null, null, null, null);
        mockRatingResponse = new RatingResponse(1L, 1L, songResponse, 5,
            LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    @WithMockUser
    @DisplayName("shouldReturn201WhenRatingWithValidData")
    void shouldReturn201WhenRatingWithValidData() throws Exception {
        RatingRequest request = new RatingRequest(10L, 5);
        when(ratingService.rate(any(RatingRequest.class))).thenReturn(mockRatingResponse);

        mockMvc.perform(post("/api/v1/ratings")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.score").value(5))
            .andExpect(jsonPath("$.song.title").value("Song A"));
    }

    @Test
    @WithMockUser
    @DisplayName("shouldReturn400WhenRatingScoreIsOutOfRange")
    void shouldReturn400WhenRatingScoreIsOutOfRange() throws Exception {
        RatingRequest request = new RatingRequest(10L, 6);

        mockMvc.perform(post("/api/v1/ratings")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("shouldReturn200WithRatingsListWhenGetMyRatings")
    void shouldReturn200WithRatingsListWhenGetMyRatings() throws Exception {
        when(ratingService.getMyRatings()).thenReturn(List.of(mockRatingResponse));

        mockMvc.perform(get("/api/v1/ratings/me"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].score").value(5))
            .andExpect(jsonPath("$[0].userId").value(1));
    }

    @Test
    @WithMockUser
    @DisplayName("shouldReturn204WhenDeleteExistingRating")
    void shouldReturn204WhenDeleteExistingRating() throws Exception {
        doNothing().when(ratingService).deleteRating(1L);

        mockMvc.perform(delete("/api/v1/ratings/1").with(csrf()))
            .andExpect(status().isNoContent());

        verify(ratingService).deleteRating(1L);
    }

    @Test
    @WithMockUser
    @DisplayName("shouldReturn404WhenDeleteNonExistingRating")
    void shouldReturn404WhenDeleteNonExistingRating() throws Exception {
        doThrow(new ResourceNotFoundException("Rating", 999L))
            .when(ratingService).deleteRating(999L);

        mockMvc.perform(delete("/api/v1/ratings/999").with(csrf()))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("shouldReturn401WhenRatingWithoutAuthentication")
    void shouldReturn401WhenRatingWithoutAuthentication() throws Exception {
        RatingRequest request = new RatingRequest(10L, 5);

        mockMvc.perform(post("/api/v1/ratings")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }
}
