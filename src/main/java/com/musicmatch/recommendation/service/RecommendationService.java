package com.musicmatch.recommendation.service;

import com.musicmatch.recommendation.dto.response.LatentProfileResponse;
import com.musicmatch.recommendation.dto.response.LatentSpaceResponse;
import com.musicmatch.recommendation.dto.response.RecommendationResponse;
import com.musicmatch.song.dto.response.SongResponse;
import com.musicmatch.recommendation.domain.LatentProfile;
import com.musicmatch.recommendation.domain.Recommendation;
import com.musicmatch.auth.domain.User;
import com.musicmatch.exceptions.ResourceNotFoundException;
import com.musicmatch.song.mapper.SongMapper;
import com.musicmatch.recommendation.repository.LatentProfileRepository;
import com.musicmatch.recommendation.repository.RecommendationRepository;
import com.musicmatch.user.repository.UserRepository;
import com.musicmatch.recommendation.service.IRecommendationService;
import com.musicmatch.auth.service.SecurityHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecommendationService implements IRecommendationService {

    private final RecommendationRepository recommendationRepository;
    private final LatentProfileRepository latentProfileRepository;
    private final UserRepository userRepository;
    private final SongMapper songMapper;
    private final SecurityHelper securityHelper;

    @Override
    public RecommendationResponse getMyRecommendations() {
        User user = securityHelper.getCurrentUser();
        Recommendation rec = recommendationRepository
            .findTopByUserIdOrderByCreatedAtDesc(user.getId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "No recommendations yet. Rate at least 5 songs to get started!"));

        List<SongResponse> songs = rec.getSongs().stream()
            .map(songMapper::toResponse).toList();
        String basedOnUserName = userRepository.findById(rec.getBasedOnUserId())
            .map(User::getName).orElse("Unknown");

        return new RecommendationResponse(
            rec.getId(), user.getId(), songs,
            rec.getBasedOnUserId(), basedOnUserName, rec.getCreatedAt()
        );
    }

    @Override
    public LatentProfileResponse getMyLatentProfile() {
        User user = securityHelper.getCurrentUser();
        LatentProfile profile = latentProfileRepository.findByUserId(user.getId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Latent profile not ready yet. Rate more songs!"));
        return new LatentProfileResponse(
            user.getId(), profile.getCoordX(), profile.getCoordY(), profile.getCoordZ(),
            profile.getClosestUserId(), profile.getCompatibilityScore(), profile.getUpdatedAt()
        );
    }

    @Override
    public LatentSpaceResponse getLatentSpace() {
        List<LatentProfile> profiles = latentProfileRepository.findAll();
        List<LatentSpaceResponse.UserLatentPoint> points = profiles.stream()
            .map(p -> new LatentSpaceResponse.UserLatentPoint(
                p.getUser().getId(), p.getUser().getName(),
                p.getCoordX(), p.getCoordY(), p.getCoordZ(),
                p.getCompatibilityScore(), p.getClosestUserId()
            )).toList();
        return new LatentSpaceResponse(points);
    }
}
