package com.musicmatch.recommendation.service;

import com.musicmatch.recommendation.dto.response.FeedItemResponse;
import com.musicmatch.recommendation.domain.LatentProfile;
import com.musicmatch.recommendation.domain.Rating;
import com.musicmatch.auth.domain.User;
import com.musicmatch.exceptions.ResourceNotFoundException;
import com.musicmatch.auth.service.SecurityHelper;
import com.musicmatch.song.mapper.SongMapper;
import com.musicmatch.recommendation.repository.LatentProfileRepository;
import com.musicmatch.recommendation.repository.RatingRepository;
import com.musicmatch.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FeedService implements com.musicmatch.recommendation.service.IFeedService {

    private final LatentProfileRepository latentProfileRepository;
    private final RatingRepository ratingRepository;
    private final SecurityHelper securityHelper;
    private final UserRepository userRepository;
    private final SongMapper songMapper;

    private static final int MAX_NEARBY_USERS = 5;
    private static final double MIN_COMPATIBILITY = 50.0;
    private static final int MAX_FEED_ITEMS = 20;

    /**
     * Returns recent ratings from users that are close in latent space.
     * "Close" = compatibilityScore >= 50% based on stored latent profiles.
     */
    public List<FeedItemResponse> getMyFeed() {
        User me = securityHelper.getCurrentUser();

        Optional<LatentProfile> myProfile = latentProfileRepository.findByUserId(me.getId());
        if (myProfile.isEmpty()) {
            return List.of(); // No SVD computed yet
        }

        // Get all other users' latent profiles
        List<LatentProfile> others = latentProfileRepository.findByUserIdNot(me.getId());

        // Filter by compatibility: keep users whose closest user is me,
        // or whose profile coords are close (use stored compatibilityScore as proxy)
        List<Long> nearbyUserIds = others.stream()
            .filter(p -> p.getCompatibilityScore() != null
                && p.getCompatibilityScore() >= MIN_COMPATIBILITY)
            .sorted(Comparator.comparingDouble(
                (LatentProfile p) -> p.getCompatibilityScore()).reversed())
            .limit(MAX_NEARBY_USERS)
            .map(p -> p.getUser().getId())
            .toList();

        if (nearbyUserIds.isEmpty()) return List.of();

        // Get recent ratings from nearby users
        List<FeedItemResponse> feed = new ArrayList<>();
        for (Long nearbyUserId : nearbyUserIds) {
            LatentProfile nearbyProfile = others.stream()
                .filter(p -> p.getUser().getId().equals(nearbyUserId))
                .findFirst().orElse(null);
            if (nearbyProfile == null) continue;

            String nearbyUserName = userRepository.findById(nearbyUserId)
                .map(User::getName).orElse("Unknown");
            double compatibility = nearbyProfile.getCompatibilityScore() != null
                ? nearbyProfile.getCompatibilityScore() : 0.0;

            List<Rating> recentRatings = ratingRepository.findByUserId(nearbyUserId)
                .stream()
                .sorted(Comparator.comparing(Rating::getCreatedAt).reversed())
                .limit(3)
                .toList();

            for (Rating rating : recentRatings) {
                feed.add(new FeedItemResponse(
                    nearbyUserId,
                    nearbyUserName,
                    compatibility,
                    songMapper.toResponse(rating.getSong()),
                    rating.getScore(),
                    rating.getCreatedAt()
                ));
            }
        }

        // Sort by most recent first
        feed.sort(Comparator.comparing(FeedItemResponse::ratedAt).reversed());
        return feed.stream().limit(MAX_FEED_ITEMS).toList();
    }

}
