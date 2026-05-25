package com.musicmatch.recommendation.controller;

import com.musicmatch.recommendation.dto.response.FeedItemResponse;
import com.musicmatch.song.dto.response.SongResponse;
import com.musicmatch.config.SecurityConfig;
import com.musicmatch.config.jwt.JwtAuthenticationFilter;
import com.musicmatch.config.jwt.JwtService;
import com.musicmatch.recommendation.service.FeedService;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FeedController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
@DisplayName("FeedController Tests")
class FeedControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private FeedService feedService;
    @MockBean private JwtService jwtService;
    @MockBean private UserDetailsService userDetailsService;

    private FeedItemResponse mockFeedItem;

    @BeforeEach
    void setUp() {
        SongResponse song = new SongResponse(10L, "Bohemian Rhapsody", "Queen",
            null, null, null, null, null, null, null);
        mockFeedItem = new FeedItemResponse(2L, "Bob", 85.0, song, 5, LocalDateTime.now());
    }

    @Test
    @WithMockUser
    @DisplayName("shouldReturn200WithFeedItemsWhenUserHasCompatibleNeighbors")
    void shouldReturn200WithFeedItemsWhenUserHasCompatibleNeighbors() throws Exception {
        when(feedService.getMyFeed()).thenReturn(List.of(mockFeedItem));

        mockMvc.perform(get("/api/v1/feed"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].userName").value("Bob"))
            .andExpect(jsonPath("$[0].compatibilityScore").value(85.0))
            .andExpect(jsonPath("$[0].song.title").value("Bohemian Rhapsody"))
            .andExpect(jsonPath("$[0].score").value(5));
    }

    @Test
    @WithMockUser
    @DisplayName("shouldReturn200WithEmptyListWhenUserHasNoLatentProfile")
    void shouldReturn200WithEmptyListWhenUserHasNoLatentProfile() throws Exception {
        when(feedService.getMyFeed()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/feed"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @WithMockUser
    @DisplayName("shouldReturn200WithMultipleFeedItemsWhenMultipleNearbyUsers")
    void shouldReturn200WithMultipleFeedItemsWhenMultipleNearbyUsers() throws Exception {
        SongResponse song2 = new SongResponse(11L, "Hotel California", "Eagles",
            null, null, null, null, null, null, null);
        FeedItemResponse item2 = new FeedItemResponse(3L, "Charlie", 72.0,
            song2, 4, LocalDateTime.now().minusMinutes(5));

        when(feedService.getMyFeed()).thenReturn(List.of(mockFeedItem, item2));

        mockMvc.perform(get("/api/v1/feed"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].userName").value("Bob"))
            .andExpect(jsonPath("$[1].userName").value("Charlie"));
    }

    @Test
    @DisplayName("shouldReturn401WhenGetFeedWithoutAuthentication")
    void shouldReturn401WhenGetFeedWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/feed"))
            .andExpect(status().isUnauthorized());
    }
}
