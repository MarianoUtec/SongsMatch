package com.musicmatch.repository;

import com.musicmatch.entity.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {
    List<Recommendation> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Recommendation> findTopByUserIdOrderByCreatedAtDesc(Long userId);
    void deleteByUserId(Long userId);
}
