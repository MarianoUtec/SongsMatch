package com.musicmatch.service.impl;

import com.musicmatch.dto.response.FeedItemResponse;
import com.musicmatch.dto.response.LatentProfileHistoryResponse;
import com.musicmatch.entity.LatentProfileHistory;
import com.musicmatch.entity.User;
import com.musicmatch.exception.ResourceNotFoundException;
import com.musicmatch.mapper.SongMapper;
import com.musicmatch.repository.LatentProfileHistoryRepository;
import com.musicmatch.repository.LatentProfileRepository;
import com.musicmatch.repository.RatingRepository;
import com.musicmatch.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LatentHistoryService implements com.musicmatch.service.interfaces.ILatentHistoryService {

    private final LatentProfileHistoryRepository historyRepository;
    private final SecurityHelper securityHelper;

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
