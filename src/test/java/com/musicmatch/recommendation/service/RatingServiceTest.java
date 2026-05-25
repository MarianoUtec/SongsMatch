package com.musicmatch.recommendation.service;

import com.musicmatch.recommendation.dto.request.RatingRequest;
import com.musicmatch.recommendation.dto.response.RatingResponse;
import com.musicmatch.song.dto.response.SongResponse;
import com.musicmatch.recommendation.domain.Rating;
import com.musicmatch.auth.domain.Role;
import com.musicmatch.song.domain.Song;
import com.musicmatch.auth.domain.User;
import com.musicmatch.exceptions.ResourceNotFoundException;
import com.musicmatch.song.mapper.SongMapper;
import com.musicmatch.recommendation.repository.RatingRepository;
import com.musicmatch.song.repository.SongRepository;
import com.musicmatch.auth.service.SecurityHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RatingService Tests")
@SuppressWarnings("null")
class RatingServiceTest {

    @Mock private RatingRepository ratingRepository;
    @Mock private SongRepository songRepository;
    @Mock private SongMapper songMapper;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private SecurityHelper securityHelper;

    @InjectMocks
    private RatingService ratingService;

    private User mockUser;
    private Song mockSong;
    private Rating mockRating;
    private SongResponse songResponse;

    @BeforeEach
    void setUp() {
        mockUser = User.builder().id(1L).name("Alice").email("alice@test.com")
            .password("pw").role(Role.USER).isActive(true).build();

        mockSong = Song.builder().id(10L).title("Song A").artist("Artist A")
            .spotifyId("sp1").build();

        mockRating = Rating.builder().id(100L).user(mockUser).song(mockSong).score(5).build();

        songResponse = new SongResponse(10L, "Song A", "Artist A",
            null, null, "sp1", null, null, null, null);
    }

    @Test
    @DisplayName("shouldCreateNewRatingWhenUserHasNotRatedSong")
    void shouldCreateNewRatingWhenUserHasNotRatedSong() {
        RatingRequest request = new RatingRequest(10L, 5);

        when(securityHelper.getCurrentUser()).thenReturn(mockUser);
        when(songRepository.findById(10L)).thenReturn(Optional.of(mockSong));
        when(ratingRepository.findByUserIdAndSongId(1L, 10L)).thenReturn(Optional.empty());
        when(ratingRepository.save(any(Rating.class))).thenReturn(mockRating);
        when(songMapper.toResponse(mockSong)).thenReturn(songResponse);

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

        when(securityHelper.getCurrentUser()).thenReturn(mockUser);
        when(songRepository.findById(10L)).thenReturn(Optional.of(mockSong));
        when(ratingRepository.findByUserIdAndSongId(1L, 10L)).thenReturn(Optional.of(existingRating));
        when(ratingRepository.save(existingRating)).thenReturn(existingRating);
        when(songMapper.toResponse(mockSong)).thenReturn(songResponse);

        ratingService.rate(request);

        assertThat(existingRating.getScore()).isEqualTo(3);
        verify(ratingRepository).save(existingRating);
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundExceptionWhenRatingSongThatDoesNotExist")
    void shouldThrowResourceNotFoundExceptionWhenRatingSongThatDoesNotExist() {
        RatingRequest request = new RatingRequest(999L, 5);

        when(securityHelper.getCurrentUser()).thenReturn(mockUser);
        when(songRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ratingService.rate(request))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("shouldReturnAllUserRatingsWhenGetMyRatings")
    void shouldReturnAllUserRatingsWhenGetMyRatings() {
        when(securityHelper.getCurrentUserId()).thenReturn(1L);
        when(ratingRepository.findByUserId(1L)).thenReturn(List.of(mockRating));
        when(songMapper.toResponse(mockSong)).thenReturn(songResponse);

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
