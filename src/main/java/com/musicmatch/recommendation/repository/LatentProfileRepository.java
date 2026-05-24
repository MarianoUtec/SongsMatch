package com.musicmatch.recommendation.repository;

import com.musicmatch.recommendation.domain.LatentProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LatentProfileRepository extends JpaRepository<LatentProfile, Long> {
    Optional<LatentProfile> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
    List<LatentProfile> findByUserIdNot(Long userId);
}
