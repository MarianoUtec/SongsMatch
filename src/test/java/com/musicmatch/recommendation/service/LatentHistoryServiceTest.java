package com.musicmatch.recommendation.service;

import com.musicmatch.recommendation.dto.response.LatentProfileHistoryResponse;
import com.musicmatch.recommendation.domain.LatentProfileHistory;
import com.musicmatch.auth.domain.Role;
import com.musicmatch.auth.domain.User;
import com.musicmatch.exceptions.ResourceNotFoundException;
import com.musicmatch.recommendation.repository.LatentProfileHistoryRepository;
import com.musicmatch.user.repository.UserRepository;
import com.musicmatch.recommendation.service.LatentHistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LatentHistoryService Tests")
class LatentHistoryServiceTest {

    @Mock private LatentProfileHistoryRepository historyRepository;
    @Mock private UserRepository userRepository;
    @Mock private SecurityContext securityContext;
    @Mock private Authentication authentication;

    @InjectMocks
    private LatentHistoryService latentHistoryService;

    private User alice;
    private LatentProfileHistory historyEntry1;
    private LatentProfileHistory historyEntry2;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("alice@test.com");

        alice = User.builder().id(1L).name("Alice").email("alice@test.com")
            .role(Role.USER).isActive(true).build();

        historyEntry1 = LatentProfileHistory.builder()
            .id(1L).user(alice)
            .coordX(0.1).coordY(0.2).coordZ(0.3)
            .closestUserId(2L).closestUserName("Bob")
            .compatibilityScore(70.0).ratingsCount(5)
            .recordedAt(LocalDateTime.now().minusDays(2)).build();

        historyEntry2 = LatentProfileHistory.builder()
            .id(2L).user(alice)
            .coordX(0.5).coordY(0.3).coordZ(0.2)
            .closestUserId(2L).closestUserName("Bob")
            .compatibilityScore(85.0).ratingsCount(12)
            .recordedAt(LocalDateTime.now().minusDays(1)).build();
    }

    // ─────────────────────────── getMyHistory ────────────────────────────

    @Test
    @DisplayName("shouldReturnFullHistoryInAscendingOrderWhenGetMyHistory")
    void shouldReturnFullHistoryInAscendingOrderWhenGetMyHistory() {
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(alice));
        when(historyRepository.findByUserIdOrderByRecordedAtAsc(1L))
            .thenReturn(List.of(historyEntry1, historyEntry2));

        List<LatentProfileHistoryResponse> history = latentHistoryService.getMyHistory();

        assertThat(history).hasSize(2);
        // First entry should be older (ascending order)
        assertThat(history.get(0).compatibilityScore()).isEqualTo(70.0);
        assertThat(history.get(0).ratingsCount()).isEqualTo(5);
        assertThat(history.get(0).closestUserName()).isEqualTo("Bob");
        // Second entry is more recent
        assertThat(history.get(1).compatibilityScore()).isEqualTo(85.0);
        assertThat(history.get(1).ratingsCount()).isEqualTo(12);
    }

    @Test
    @DisplayName("shouldReturnEmptyListWhenGetMyHistoryWithNoSnapshots")
    void shouldReturnEmptyListWhenGetMyHistoryWithNoSnapshots() {
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(alice));
        when(historyRepository.findByUserIdOrderByRecordedAtAsc(1L)).thenReturn(List.of());

        List<LatentProfileHistoryResponse> history = latentHistoryService.getMyHistory();

        assertThat(history).isEmpty();
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundExceptionWhenGetMyHistoryAndUserNotFound")
    void shouldThrowResourceNotFoundExceptionWhenGetMyHistoryAndUserNotFound() {
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> latentHistoryService.getMyHistory())
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("shouldMapAllFieldsCorrectlyWhenGetMyHistory")
    void shouldMapAllFieldsCorrectlyWhenGetMyHistory() {
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(alice));
        when(historyRepository.findByUserIdOrderByRecordedAtAsc(1L))
            .thenReturn(List.of(historyEntry1));

        List<LatentProfileHistoryResponse> history = latentHistoryService.getMyHistory();

        LatentProfileHistoryResponse response = history.get(0);
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.coordX()).isEqualTo(0.1);
        assertThat(response.coordY()).isEqualTo(0.2);
        assertThat(response.coordZ()).isEqualTo(0.3);
        assertThat(response.closestUserId()).isEqualTo(2L);
        assertThat(response.closestUserName()).isEqualTo("Bob");
        assertThat(response.compatibilityScore()).isEqualTo(70.0);
        assertThat(response.ratingsCount()).isEqualTo(5);
        assertThat(response.recordedAt()).isNotNull();
    }

    // ─────────────────────────── getRecentHistory ────────────────────────

    @Test
    @DisplayName("shouldReturnTop10HistoryEntriesWhenGetRecentHistory")
    void shouldReturnTop10HistoryEntriesWhenGetRecentHistory() {
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(alice));
        when(historyRepository.findTop10ByUserIdOrderByRecordedAtDesc(1L))
            .thenReturn(List.of(historyEntry2, historyEntry1)); // descending: newest first

        List<LatentProfileHistoryResponse> recentHistory = latentHistoryService.getRecentHistory();

        assertThat(recentHistory).hasSize(2);
        // Newest entry first (descending)
        assertThat(recentHistory.get(0).compatibilityScore()).isEqualTo(85.0);
        assertThat(recentHistory.get(1).compatibilityScore()).isEqualTo(70.0);
        verify(historyRepository).findTop10ByUserIdOrderByRecordedAtDesc(1L);
    }

    @Test
    @DisplayName("shouldReturnEmptyListWhenGetRecentHistoryWithNoSnapshots")
    void shouldReturnEmptyListWhenGetRecentHistoryWithNoSnapshots() {
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(alice));
        when(historyRepository.findTop10ByUserIdOrderByRecordedAtDesc(1L)).thenReturn(List.of());

        List<LatentProfileHistoryResponse> recentHistory = latentHistoryService.getRecentHistory();

        assertThat(recentHistory).isEmpty();
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundExceptionWhenGetRecentHistoryAndUserNotFound")
    void shouldThrowResourceNotFoundExceptionWhenGetRecentHistoryAndUserNotFound() {
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> latentHistoryService.getRecentHistory())
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("shouldCallCorrectRepositoryMethodWhenGetRecentHistoryVsGetMyHistory")
    void shouldCallCorrectRepositoryMethodWhenGetRecentHistoryVsGetMyHistory() {
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(alice));
        when(historyRepository.findTop10ByUserIdOrderByRecordedAtDesc(1L)).thenReturn(List.of());

        latentHistoryService.getRecentHistory();

        verify(historyRepository).findTop10ByUserIdOrderByRecordedAtDesc(1L);
        verify(historyRepository, never()).findByUserIdOrderByRecordedAtAsc(anyLong());
    }
}
