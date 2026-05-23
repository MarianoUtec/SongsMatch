package com.musicmatch.service.impl;

import com.musicmatch.dto.response.LatentProfileResponse;
import com.musicmatch.dto.response.LatentSpaceResponse;
import com.musicmatch.dto.response.RecommendationResponse;
import com.musicmatch.dto.response.SongResponse;
import com.musicmatch.entity.LatentProfile;
import com.musicmatch.entity.Recommendation;
import com.musicmatch.entity.User;
import com.musicmatch.exception.ResourceNotFoundException;
import com.musicmatch.mapper.SongMapper;
import com.musicmatch.repository.LatentProfileRepository;
import com.musicmatch.repository.RecommendationRepository;
import com.musicmatch.repository.UserRepository;
import com.musicmatch.service.interfaces.IRecommendationService;
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
