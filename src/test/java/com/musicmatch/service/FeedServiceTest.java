package com.musicmatch.service;

import com.musicmatch.dto.response.FeedItemResponse;
import com.musicmatch.dto.response.SongResponse;
import com.musicmatch.entity.*;
import com.musicmatch.exception.ResourceNotFoundException;
import com.musicmatch.mapper.SongMapper;
import com.musicmatch.repository.LatentProfileRepository;
import com.musicmatch.repository.RatingRepository;
import com.musicmatch.repository.UserRepository;
import com.musicmatch.service.impl.FeedService;
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
@DisplayName("FeedService Tests")
class FeedServiceTest {

    @Mock private LatentProfileRepository latentProfileRepository;
    @Mock private RatingRepository ratingRepository;
    @Mock private UserRepository userRepository;
    @Mock private SongMapper songMapper;
    @Mock private SecurityContext securityContext;
    @Mock private Authentication authentication;

    @InjectMocks
    private FeedService feedService;

    private User alice;
    private User bob;
    private LatentProfile aliceProfile;
    private LatentProfile bobProfile;
    private Song song;
    private Rating rating;
    private SongResponse songResponse;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("alice@test.com");

        alice = User.builder().id(1L).name("Alice").email("alice@test.com")
            .role(Role.USER).isActive(true).build();
        bob = User.builder().id(2L).name("Bob").email("bob@test.com")
            .role(Role.USER).isActive(true).build();

        aliceProfile = LatentProfile.builder()
            .id(1L).user(alice)
            .coordX(0.5).coordY(0.3).coordZ(0.2)
            .closestUserId(2L).compatibilityScore(85.0)
            .updatedAt(LocalDateTime.now()).build();

        bobProfile = LatentProfile.builder()
            .id(2L).user(bob)
            .coordX(0.4).coordY(0.3).coordZ(0.2)
            .closestUserId(1L).compatibilityScore(85.0)
            .updatedAt(LocalDateTime.now()).build();

        song = Song.builder().id(10L).title("Bohemian Rhapsody").artist("Queen").build();
        rating = Rating.builder().id(100L).user(bob).song(song).score(5)
            .createdAt(LocalDateTime.now()).build();
        songResponse = new SongResponse(10L, "Bohemian Rhapsody", "Queen",
            null, null, null, null, null, null, null);
    }

    // ─────────────────────────── getMyFeed ───────────────────────────────

    @Test
    @DisplayName("shouldReturnFeedItemsWhenUserHasLatentProfileAndNearbyUsers")
    void shouldReturnFeedItemsWhenUserHasLatentProfileAndNearbyUsers() {
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(alice));
        when(latentProfileRepository.findByUserId(1L)).thenReturn(Optional.of(aliceProfile));
        when(latentProfileRepository.findByUserIdNot(1L)).thenReturn(List.of(bobProfile));
        when(userRepository.findById(2L)).thenReturn(Optional.of(bob));
        when(ratingRepository.findByUserId(2L)).thenReturn(List.of(rating));
        when(songMapper.toResponse(song)).thenReturn(songResponse);

        List<FeedItemResponse> feed = feedService.getMyFeed();

        assertThat(feed).hasSize(1);
        assertThat(feed.get(0).userName()).isEqualTo("Bob");
        assertThat(feed.get(0).compatibilityScore()).isEqualTo(85.0);
        assertThat(feed.get(0).song().title()).isEqualTo("Bohemian Rhapsody");
        assertThat(feed.get(0).score()).isEqualTo(5);
    }

    @Test
    @DisplayName("shouldReturnEmptyFeedWhenUserHasNoLatentProfile")
    void shouldReturnEmptyFeedWhenUserHasNoLatentProfile() {
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(alice));
        when(latentProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());

        List<FeedItemResponse> feed = feedService.getMyFeed();

        assertThat(feed).isEmpty();
        verify(latentProfileRepository, never()).findByUserIdNot(anyLong());
    }

    @Test
    @DisplayName("shouldReturnEmptyFeedWhenNoNearbyUsersHaveHighEnoughCompatibility")
    void shouldReturnEmptyFeedWhenNoNearbyUsersHaveHighEnoughCompatibility() {
        LatentProfile lowCompatProfile = LatentProfile.builder()
            .id(2L).user(bob)
            .coordX(0.9).coordY(0.9).coordZ(0.9)
            .closestUserId(1L).compatibilityScore(20.0) // below MIN_COMPATIBILITY of 50.0
            .updatedAt(LocalDateTime.now()).build();

        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(alice));
        when(latentProfileRepository.findByUserId(1L)).thenReturn(Optional.of(aliceProfile));
        when(latentProfileRepository.findByUserIdNot(1L)).thenReturn(List.of(lowCompatProfile));

        List<FeedItemResponse> feed = feedService.getMyFeed();

        assertThat(feed).isEmpty();
        verify(ratingRepository, never()).findByUserId(anyLong());
    }

    @Test
    @DisplayName("shouldReturnEmptyFeedWhenNearbyUserHasNullCompatibilityScore")
    void shouldReturnEmptyFeedWhenNearbyUserHasNullCompatibilityScore() {
        LatentProfile nullScoreProfile = LatentProfile.builder()
            .id(2L).user(bob)
            .coordX(0.4).coordY(0.3).coordZ(0.2)
            .closestUserId(1L).compatibilityScore(null) // null score must be filtered
            .updatedAt(LocalDateTime.now()).build();

        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(alice));
        when(latentProfileRepository.findByUserId(1L)).thenReturn(Optional.of(aliceProfile));
        when(latentProfileRepository.findByUserIdNot(1L)).thenReturn(List.of(nullScoreProfile));

        List<FeedItemResponse> feed = feedService.getMyFeed();

        assertThat(feed).isEmpty();
    }

    @Test
    @DisplayName("shouldReturnAtMostThreeRatingsPerUserWhenGetMyFeed")
    void shouldReturnAtMostThreeRatingsPerUserWhenGetMyFeed() {
        Song song2 = Song.builder().id(11L).title("Song 2").artist("Artist 2").build();
        Song song3 = Song.builder().id(12L).title("Song 3").artist("Artist 3").build();
        Song song4 = Song.builder().id(13L).title("Song 4").artist("Artist 4").build();

        Rating r2 = Rating.builder().id(101L).user(bob).song(song2).score(4)
            .createdAt(LocalDateTime.now().minusMinutes(1)).build();
        Rating r3 = Rating.builder().id(102L).user(bob).song(song3).score(3)
            .createdAt(LocalDateTime.now().minusMinutes(2)).build();
        Rating r4 = Rating.builder().id(103L).user(bob).song(song4).score(2)
            .createdAt(LocalDateTime.now().minusMinutes(3)).build();

        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(alice));
        when(latentProfileRepository.findByUserId(1L)).thenReturn(Optional.of(aliceProfile));
        when(latentProfileRepository.findByUserIdNot(1L)).thenReturn(List.of(bobProfile));
        when(userRepository.findById(2L)).thenReturn(Optional.of(bob));
        when(ratingRepository.findByUserId(2L)).thenReturn(List.of(rating, r2, r3, r4));
        when(songMapper.toResponse(any(Song.class))).thenReturn(songResponse);

        List<FeedItemResponse> feed = feedService.getMyFeed();

        // Should cap at 3 ratings per user
        assertThat(feed).hasSize(3);
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundExceptionWhenGetMyFeedAndUserNotFound")
    void shouldThrowResourceNotFoundExceptionWhenGetMyFeedAndUserNotFound() {
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> feedService.getMyFeed())
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
