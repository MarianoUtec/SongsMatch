package com.musicmatch.recommendation.service.async;

import com.musicmatch.algorithm.SvdAlgorithm;
import com.musicmatch.algorithm.SvdAlgorithm.SvdResult;
import com.musicmatch.auth.domain.User;
import com.musicmatch.recommendation.domain.LatentProfile;
import com.musicmatch.recommendation.domain.LatentProfileHistory;
import com.musicmatch.recommendation.domain.Rating;
import com.musicmatch.recommendation.domain.Recommendation;
import com.musicmatch.song.domain.Song;
import com.musicmatch.user.repository.UserRepository;
import com.musicmatch.recommendation.repository.LatentProfileHistoryRepository;
import com.musicmatch.recommendation.repository.LatentProfileRepository;
import com.musicmatch.recommendation.repository.RatingRepository;
import com.musicmatch.recommendation.repository.RecommendationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SvdComputationService {

    private final RatingRepository ratingRepository;
    private final LatentProfileRepository latentProfileRepository;
    private final LatentProfileHistoryRepository latentProfileHistoryRepository;
    private final RecommendationRepository recommendationRepository;
    private final UserRepository userRepository;
    private final SvdAlgorithm svdAlgorithm;
    private final EmailService emailService;

    @Async("taskExecutor")
    @Transactional
    public void recomputeForAllUsers() {
        try {
            List<Rating> allRatings = ratingRepository.findAllWithUserAndSong();
            if (allRatings.size() < 2) {
                log.warn("Not enough ratings for SVD computation");
                return;
            }

            SvdResult result = svdAlgorithm.compute(allRatings);
            updateLatentProfiles(result, allRatings);
            List<Long> usersWithNewRecs = generateRecommendations(result, allRatings);
            notifyUsersWithRecommendations(usersWithNewRecs);

            log.info("SVD recomputation completed for {} users", result.userIds().size());
        } catch (Exception e) {
            log.error("SVD recomputation failed: {}", e.getMessage());
        }
    }

    private void updateLatentProfiles(SvdResult result, List<Rating> allRatings) {
        List<Long> userIds = result.userIds();
        double[][] U = result.U();

        // Build a map of ratings count per user
        Map<Long, Long> ratingsCounts = allRatings.stream()
            .collect(Collectors.groupingBy(r -> r.getUser().getId(), Collectors.counting()));

        for (int i = 0; i < userIds.size(); i++) {
            Long userId = userIds.get(i);
            Long currentUserId = Objects.requireNonNull(userId);
            int closestIdx = svdAlgorithm.findClosestUserIndex(U, i);
            double similarity = closestIdx >= 0
                ? svdAlgorithm.cosineSimilarity(U[i], U[closestIdx]) : 0;
            double compatibilityScore = svdAlgorithm.toCompatibilityPercent(similarity);
            Long closestUserId = closestIdx >= 0 ? userIds.get(closestIdx) : null;

            final double[] row = U[i];

            // Update current LatentProfile
            LatentProfile profile = latentProfileRepository.findByUserId(currentUserId)
                .orElse(LatentProfile.builder()
                    .user(userRepository.getReferenceById(currentUserId))
                    .build());

            profile.setCoordX(row.length > 0 ? row[0] : 0);
            profile.setCoordY(row.length > 1 ? row[1] : 0);
            profile.setCoordZ(row.length > 2 ? row[2] : 0);
            profile.setClosestUserId(closestUserId);
            profile.setCompatibilityScore(compatibilityScore);
            latentProfileRepository.save(profile);

            // Save snapshot to history
            String closestUserName = closestUserId != null
                ? userRepository.findById(closestUserId).map(User::getName).orElse("Unknown")
                : null;

            LatentProfileHistory history = Objects.requireNonNull(LatentProfileHistory.builder()
                .user(userRepository.getReferenceById(currentUserId))
                .coordX(row.length > 0 ? row[0] : 0)
                .coordY(row.length > 1 ? row[1] : 0)
                .coordZ(row.length > 2 ? row[2] : 0)
                .closestUserId(closestUserId)
                .closestUserName(closestUserName)
                .compatibilityScore(compatibilityScore)
                .ratingsCount(ratingsCounts.getOrDefault(userId, 0L).intValue())
                .build());
            latentProfileHistoryRepository.save(history);
        }
    }

    private List<Long> generateRecommendations(SvdResult result, List<Rating> allRatings) {
        List<Long> userIds = result.userIds();
        double[][] U = result.U();
        List<Long> usersWithNewRecs = new ArrayList<>();

        for (int i = 0; i < userIds.size(); i++) {
            Long userId = userIds.get(i);
            Long currentUserId = Objects.requireNonNull(userId);
            int closestIdx = svdAlgorithm.findClosestUserIndex(U, i);
            if (closestIdx < 0) continue;

            Long closestUserId = Objects.requireNonNull(userIds.get(closestIdx));

            Set<Long> myRatedSongIds = allRatings.stream()
                .filter(r -> r.getUser().getId().equals(currentUserId))
                .map(r -> r.getSong().getId())
                .collect(Collectors.toSet());

            List<Song> recommendations = allRatings.stream()
                .filter(r -> r.getUser().getId().equals(closestUserId))
                .filter(r -> !myRatedSongIds.contains(r.getSong().getId()))
                .sorted((a, b) -> b.getScore() - a.getScore())
                .limit(2)
                .map(Rating::getSong)
                .collect(Collectors.toList());

            if (!recommendations.isEmpty()) {
                recommendationRepository.deleteByUserId(currentUserId);
                Recommendation rec = Objects.requireNonNull(Recommendation.builder()
                    .user(userRepository.getReferenceById(currentUserId))
                    .songs(recommendations)
                    .basedOnUserId(closestUserId)
                    .build());
                recommendationRepository.save(rec);
                usersWithNewRecs.add(currentUserId);
            }
        }
        return usersWithNewRecs;
    }

    private void notifyUsersWithRecommendations(List<Long> userIds) {
        for (Long userId : userIds) {
            Long currentUserId = Objects.requireNonNull(userId);
            userRepository.findById(currentUserId).ifPresent(user ->
                recommendationRepository
                    .findTopByUserIdOrderByCreatedAtDesc(currentUserId)
                    .ifPresent(rec -> {
                        User matchedUser = userRepository
                            .findById(Objects.requireNonNull(rec.getBasedOnUserId())).orElse(null);
                        emailService.sendRecommendationReadyEmail(
                            user, rec.getSongs(), matchedUser,
                            latentProfileRepository.findByUserId(currentUserId)
                                .map(LatentProfile::getCompatibilityScore).orElse(0.0)
                        );
                    })
            );
        }
    }
}
