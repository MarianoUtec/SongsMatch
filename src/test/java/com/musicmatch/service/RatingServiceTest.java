package com.musicmatch.service;

import com.musicmatch.dto.request.RatingRequest;
import com.musicmatch.dto.response.RatingResponse;
import com.musicmatch.dto.response.SongResponse;
import com.musicmatch.entity.Rating;
import com.musicmatch.entity.Role;
import com.musicmatch.entity.Song;
import com.musicmatch.entity.User;
import com.musicmatch.exception.ResourceNotFoundException;
import com.musicmatch.mapper.RatingMapper;
import com.musicmatch.repository.RatingRepository;
import com.musicmatch.repository.SongRepository;
import com.musicmatch.repository.UserRepository;
import com.musicmatch.service.impl.RatingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RatingService Tests")
class RatingServiceTest {

    @Mock private RatingRepository ratingRepository;
    @Mock private UserRepository userRepository;
    @Mock private SongRepository songRepository;
    @Mock private RatingMapper ratingMapper;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private SecurityContext securityContext;
    @Mock private Authentication authentication;

    @InjectMocks
    private RatingService ratingService;

    private User mockUser;
    private Song mockSong;
    private Rating mockRating;
    private RatingResponse mockRatingResponse;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("alice@test.com");

        mockUser = User.builder().id(1L).name("Alice").email("alice@test.com")
            .password("pw").role(Role.USER).isActive(true).build();

        mockSong = Song.builder().id(10L).title("Song A").artist("Artist A")
            .spotifyId("sp1").build();

        mockRating = Rating.builder().id(100L).user(mockUser).song(mockSong).score(5).build();

        SongResponse songResponse = new SongResponse(10L, "Song A", "Artist A",
            null, null, "sp1", null, null, null, null);
        mockRatingResponse = new RatingResponse(100L, 1L, songResponse, 5,
            LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    @DisplayName("shouldCreateNewRatingWhenUserHasNotRatedSong")
    void shouldCreateNewRatingWhenUserHasNotRatedSong() {
        RatingRequest request = new RatingRequest(10L, 5);

        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(mockUser));
        when(songRepository.findById(10L)).thenReturn(Optional.of(mockSong));
        when(ratingRepository.findByUserIdAndSongId(1L, 10L)).thenReturn(Optional.empty());
        when(ratingRepository.save(any(Rating.class))).thenReturn(mockRating);
        when(ratingMapper.toResponse(mockRating)).thenReturn(mockRatingResponse);

        RatingResponse response = ratingService.rate(request);

        assertThat(response.score()).isEqualTo(5);
        verify(ratingRepository).save(any(Rating.class));
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    @DisplayName("shouldUpdateExistingRatingWhenUserRatesSongAgain")
    void shouldUpdateExistingRatingWhenUserRatesSongAgain() {
        RatingRequest request = new RatingRequest(10L, 3);
        Rating existingRating = Rating.builder().id(100L).user(mockUser).song(mockSong).score(5).build();

        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(mockUser));
        when(songRepository.findById(10L)).thenReturn(Optional.of(mockSong));
        when(ratingRepository.findByUserIdAndSongId(1L, 10L)).thenReturn(Optional.of(existingRating));
        when(ratingRepository.save(existingRating)).thenReturn(existingRating);
        when(ratingMapper.toResponse(existingRating)).thenReturn(mockRatingResponse);

        ratingService.rate(request);

        assertThat(existingRating.getScore()).isEqualTo(3);
        verify(ratingRepository).save(existingRating);
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundExceptionWhenRatingSongThatDoesNotExist")
    void shouldThrowResourceNotFoundExceptionWhenRatingSongThatDoesNotExist() {
        RatingRequest request = new RatingRequest(999L, 5);

        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(mockUser));
        when(songRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ratingService.rate(request))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("shouldReturnAllUserRatingsWhenGetMyRatings")
    void shouldReturnAllUserRatingsWhenGetMyRatings() {
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(mockUser));
        when(ratingRepository.findByUserId(1L)).thenReturn(List.of(mockRating));
        when(ratingMapper.toResponse(mockRating)).thenReturn(mockRatingResponse);

        List<RatingResponse> ratings = ratingService.getMyRatings();

        assertThat(ratings).hasSize(1);
        assertThat(ratings.get(0).score()).isEqualTo(5);
    }

    @Test
    @DisplayName("shouldDeleteRatingWhenDeleteRatingWithValidId")
    void shouldDeleteRatingWhenDeleteRatingWithValidId() {
        when(ratingRepository.findById(100L)).thenReturn(Optional.of(mockRating));

        ratingService.deleteRating(100L);

        verify(ratingRepository).delete(mockRating);
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundExceptionWhenDeleteRatingWithInvalidId")
    void shouldThrowResourceNotFoundExceptionWhenDeleteRatingWithInvalidId() {
        when(ratingRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ratingService.deleteRating(999L))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
