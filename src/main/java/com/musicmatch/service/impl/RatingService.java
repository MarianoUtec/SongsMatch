package com.musicmatch.service.impl;

import com.musicmatch.dto.request.RatingRequest;
import com.musicmatch.dto.response.RatingResponse;
import com.musicmatch.dto.response.SongResponse;
import com.musicmatch.entity.Rating;
import com.musicmatch.entity.Song;
import com.musicmatch.entity.User;
import com.musicmatch.event.RatingSubmittedEvent;
import com.musicmatch.exception.ResourceNotFoundException;
import com.musicmatch.mapper.SongMapper;
import com.musicmatch.repository.RatingRepository;
import com.musicmatch.repository.SongRepository;
import com.musicmatch.service.interfaces.IRatingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
        Song song = songRepository.findById(request.songId())
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
        Rating rating = ratingRepository.findById(ratingId)
            .orElseThrow(() -> new ResourceNotFoundException("Rating", ratingId));
        ratingRepository.delete(rating);
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
