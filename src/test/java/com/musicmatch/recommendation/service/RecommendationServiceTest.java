package com.musicmatch.recommendation.service;

import com.musicmatch.recommendation.dto.response.LatentProfileResponse;
import com.musicmatch.recommendation.dto.response.LatentSpaceResponse;
import com.musicmatch.recommendation.dto.response.RecommendationResponse;
import com.musicmatch.song.dto.response.SongResponse;
import com.musicmatch.auth.domain.Role;
import com.musicmatch.auth.domain.User;
import com.musicmatch.recommendation.domain.LatentProfile;
import com.musicmatch.recommendation.domain.Recommendation;
import com.musicmatch.song.domain.Song;
import com.musicmatch.exceptions.ResourceNotFoundException;
import com.musicmatch.song.mapper.SongMapper;
import com.musicmatch.recommendation.repository.LatentProfileRepository;
import com.musicmatch.recommendation.repository.RecommendationRepository;
import com.musicmatch.user.repository.UserRepository;
import com.musicmatch.auth.service.SecurityHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecommendationService Tests")
class RecommendationServiceTest {

    @Mock private RecommendationRepository recommendationRepository;
    @Mock private LatentProfileRepository latentProfileRepository;
    @Mock private UserRepository userRepository;
    @Mock private SongMapper songMapper;
    @Mock private SecurityHelper securityHelper;

    @InjectMocks
    private RecommendationService recommendationService;

    private User currentUser;
    private User similarUser;
    private Song recommendedSong;
    private Recommendation recommendation;
    private LatentProfile latentProfile;

    @BeforeEach
    void setUp() {
        currentUser = User.builder().id(1L).name("Alice").email("alice@test.com")
            .role(Role.USER).isActive(true).build();
        similarUser = User.builder().id(2L).name("Bob").email("bob@test.com")
            .role(Role.USER).isActive(true).build();

        recommendedSong = Song.builder().id(10L).title("Song A").artist("Artist A").build();

        recommendation = Recommendation.builder()
            .id(1L).user(currentUser).songs(List.of(recommendedSong))
            .basedOnUserId(2L).createdAt(LocalDateTime.now()).build();

        latentProfile = LatentProfile.builder()
            .id(1L).user(currentUser)
            .coordX(0.5).coordY(0.3).coordZ(0.2)
            .closestUserId(2L).compatibilityScore(87.0)
            .updatedAt(LocalDateTime.now()).build();

        lenient().when(securityHelper.getCurrentUser()).thenReturn(currentUser);
    }

    @Test
    @DisplayName("shouldReturnRecommendationsWhenUserHasProfile")
    void shouldReturnRecommendationsWhenUserHasProfile() {
        SongResponse songResponse = new SongResponse(10L, "Song A", "Artist A",
            null, null, null, null, null, null, null);

        when(recommendationRepository.findTopByUserIdOrderByCreatedAtDesc(1L))
            .thenReturn(Optional.of(recommendation));
        when(userRepository.findById(2L)).thenReturn(Optional.of(similarUser));
        when(songMapper.toResponse(recommendedSong)).thenReturn(songResponse);

        RecommendationResponse response = recommendationService.getMyRecommendations();

        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.songs()).hasSize(1);
        assertThat(response.basedOnUserName()).isEqualTo("Bob");
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundExceptionWhenNoRecommendationsExist")
    void shouldThrowResourceNotFoundExceptionWhenNoRecommendationsExist() {
        when(recommendationRepository.findTopByUserIdOrderByCreatedAtDesc(1L))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> recommendationService.getMyRecommendations())
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("shouldReturnLatentProfileWhenUserHasBeenProcessed")
    void shouldReturnLatentProfileWhenUserHasBeenProcessed() {
        when(latentProfileRepository.findByUserId(1L)).thenReturn(Optional.of(latentProfile));

        LatentProfileResponse response = recommendationService.getMyLatentProfile();

        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.coordX()).isEqualTo(0.5);
        assertThat(response.coordY()).isEqualTo(0.3);
        assertThat(response.coordZ()).isEqualTo(0.2);
        assertThat(response.compatibilityScore()).isEqualTo(87.0);
        assertThat(response.closestUserId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundExceptionWhenLatentProfileNotReady")
    void shouldThrowResourceNotFoundExceptionWhenLatentProfileNotReady() {
        when(latentProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recommendationService.getMyLatentProfile())
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("shouldReturnAllLatentPointsWhenGetLatentSpace")
    void shouldReturnAllLatentPointsWhenGetLatentSpace() {
        LatentProfile profile2 = LatentProfile.builder()
            .id(2L).user(similarUser)
            .coordX(0.1).coordY(0.9).coordZ(0.4)
            .compatibilityScore(87.0).closestUserId(1L).build();

        when(latentProfileRepository.findAll()).thenReturn(List.of(latentProfile, profile2));

        LatentSpaceResponse response = recommendationService.getLatentSpace();

        assertThat(response.users()).hasSize(2);
        assertThat(response.users()).extracting(LatentSpaceResponse.UserLatentPoint::userName)
            .containsExactlyInAnyOrder("Alice", "Bob");
    }
}
