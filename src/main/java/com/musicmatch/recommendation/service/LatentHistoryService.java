package com.musicmatch.recommendation.service;

import com.musicmatch.recommendation.dto.response.FeedItemResponse;
import com.musicmatch.recommendation.dto.response.LatentProfileHistoryResponse;
import com.musicmatch.recommendation.domain.LatentProfileHistory;
import com.musicmatch.auth.domain.User;
import com.musicmatch.exceptions.ResourceNotFoundException;
import com.musicmatch.song.mapper.SongMapper;
import com.musicmatch.recommendation.repository.LatentProfileHistoryRepository;
import com.musicmatch.recommendation.repository.LatentProfileRepository;
import com.musicmatch.recommendation.repository.RatingRepository;
import com.musicmatch.user.repository.UserRepository;
import com.musicmatch.auth.service.SecurityHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LatentHistoryService implements com.musicmatch.recommendation.service.ILatentHistoryService {

    private final LatentProfileHistoryRepository historyRepository;
    private final SecurityHelper securityHelper;
    private final UserRepository userRepository;

    public List<LatentProfileHistoryResponse> getMyHistory() {
        User me = securityHelper.getCurrentUser();
        return historyRepository.findByUserIdOrderByRecordedAtAsc(me.getId())
            .stream()
            .map(this::toResponse)
            .toList();
    }

    public List<LatentProfileHistoryResponse> getRecentHistory() {
        User me = securityHelper.getCurrentUser();
        return historyRepository.findTop10ByUserIdOrderByRecordedAtDesc(me.getId())
            .stream()
            .map(this::toResponse)
            .toList();
    }

    private LatentProfileHistoryResponse toResponse(LatentProfileHistory h) {
        return new LatentProfileHistoryResponse(
            h.getId(), h.getCoordX(), h.getCoordY(), h.getCoordZ(),
            h.getClosestUserId(), h.getClosestUserName(),
            h.getCompatibilityScore(), h.getRatingsCount(), h.getRecordedAt()
        );
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
