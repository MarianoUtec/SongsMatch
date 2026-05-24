package com.musicmatch.recommendation.repository;

import com.musicmatch.recommendation.domain.LatentProfileHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LatentProfileHistoryRepository extends JpaRepository<LatentProfileHistory, Long> {

    List<LatentProfileHistory> findByUserIdOrderByRecordedAtAsc(Long userId);

    List<LatentProfileHistory> findTop10ByUserIdOrderByRecordedAtDesc(Long userId);
}
