package com.musicmatch.controller;

import com.musicmatch.dto.response.LatentProfileHistoryResponse;
import com.musicmatch.security.filter.JwtAuthenticationFilter;
import com.musicmatch.security.service.JwtService;
import com.musicmatch.service.impl.LatentHistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LatentHistoryController.class)
@DisplayName("LatentHistoryController Tests")
class LatentHistoryControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private LatentHistoryService latentHistoryService;
    @MockBean private JwtService jwtService;
    @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean private UserDetailsService userDetailsService;

    private LatentProfileHistoryResponse entry1;
    private LatentProfileHistoryResponse entry2;

    @BeforeEach
    void setUp() {
        entry1 = new LatentProfileHistoryResponse(
            1L, 0.1, 0.2, 0.3, 2L, "Bob", 70.0, 5,
            LocalDateTime.now().minusDays(2));
        entry2 = new LatentProfileHistoryResponse(
            2L, 0.5, 0.3, 0.2, 2L, "Bob", 85.0, 12,
            LocalDateTime.now().minusDays(1));
    }

    @Test
    @WithMockUser
    @DisplayName("shouldReturn200WithFullHistoryWhenGetFullHistory")
    void shouldReturn200WithFullHistoryWhenGetFullHistory() throws Exception {
        when(latentHistoryService.getMyHistory()).thenReturn(List.of(entry1, entry2));

        mockMvc.perform(get("/api/v1/users/me/twin-history"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].compatibilityScore").value(70.0))
            .andExpect(jsonPath("$[0].ratingsCount").value(5))
            .andExpect(jsonPath("$[0].closestUserName").value("Bob"))
            .andExpect(jsonPath("$[1].compatibilityScore").value(85.0));
    }

    @Test
    @WithMockUser
    @DisplayName("shouldReturn200WithEmptyListWhenUserHasNoHistoryYet")
    void shouldReturn200WithEmptyListWhenUserHasNoHistoryYet() throws Exception {
        when(latentHistoryService.getMyHistory()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/users/me/twin-history"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @WithMockUser
    @DisplayName("shouldReturn200WithRecentHistoryWhenGetRecentHistory")
    void shouldReturn200WithRecentHistoryWhenGetRecentHistory() throws Exception {
        // Descending order: newest first
        when(latentHistoryService.getRecentHistory()).thenReturn(List.of(entry2, entry1));

        mockMvc.perform(get("/api/v1/users/me/twin-history/recent"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].compatibilityScore").value(85.0))
            .andExpect(jsonPath("$[0].ratingsCount").value(12))
            .andExpect(jsonPath("$[1].compatibilityScore").value(70.0));
    }

    @Test
    @WithMockUser
    @DisplayName("shouldReturn200WithEmptyListWhenRecentHistoryIsEmpty")
    void shouldReturn200WithEmptyListWhenRecentHistoryIsEmpty() throws Exception {
        when(latentHistoryService.getRecentHistory()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/users/me/twin-history/recent"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("shouldReturn401WhenGetHistoryWithoutAuthentication")
    void shouldReturn401WhenGetHistoryWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/twin-history"))
            .andExpect(status().isUnauthorized());
    }
}
