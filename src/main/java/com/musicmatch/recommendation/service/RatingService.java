package com.musicmatch.recommendation.service;

import com.musicmatch.recommendation.dto.request.RatingRequest;
import com.musicmatch.recommendation.dto.response.RatingResponse;
import com.musicmatch.song.dto.response.SongResponse;
import com.musicmatch.recommendation.domain.Rating;
import com.musicmatch.song.domain.Song;
import com.musicmatch.auth.domain.User;
import com.musicmatch.events.RatingSubmittedEvent;
import com.musicmatch.exceptions.ResourceNotFoundException;
import com.musicmatch.song.mapper.SongMapper;
import com.musicmatch.recommendation.repository.RatingRepository;
import com.musicmatch.song.repository.SongRepository;
import com.musicmatch.auth.service.SecurityHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class RatingService implements IRatingService {

    private final RatingRepository ratingRepository;
    private final SongRepository songRepository;
    private final SongMapper songMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final SecurityHelper securityHelper;

    @Override
    @Transactional
    public RatingResponse rate(RatingRequest request) {
        User user = securityHelper.getCurrentUser();
        Long songId = Objects.requireNonNull(request.songId());
        Song song = songRepository.findById(songId)
            .orElseThrow(() -> new ResourceNotFoundException("Song", request.songId()));

        Rating rating = ratingRepository.findByUserIdAndSongId(user.getId(), song.getId())
            .orElse(Rating.builder().user(user).song(song).build());

        rating.setScore(request.score());
        rating = ratingRepository.save(rating);
        eventPublisher.publishEvent(new RatingSubmittedEvent(this, rating));
        return buildResponse(rating, song);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RatingResponse> getMyRatings() {
        Long userId = securityHelper.getCurrentUserId();
        return ratingRepository.findByUserId(userId).stream()
            .map(r -> buildResponse(r, r.getSong()))
            .toList();
    }

    @Override
    @Transactional
    public void deleteRating(Long ratingId) {
        Long currentRatingId = Objects.requireNonNull(ratingId);
        Rating rating = ratingRepository.findById(currentRatingId)
            .orElseThrow(() -> new ResourceNotFoundException("Rating", ratingId));
        ratingRepository.delete(Objects.requireNonNull(rating));
    }

    private RatingResponse buildResponse(Rating rating, Song song) {
        SongResponse songResponse = songMapper.toResponse(song);
        return new RatingResponse(
            rating.getId(),
            rating.getUser().getId(),
            songResponse,
            rating.getScore(),
            rating.getCreatedAt(),
            rating.getUpdatedAt()
        );
    }
}
