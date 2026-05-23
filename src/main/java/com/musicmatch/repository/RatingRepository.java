package com.musicmatch.repository;

import com.musicmatch.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {
    List<Rating> findByUserId(Long userId);
    List<Rating> findBySongId(Long songId);
    Optional<Rating> findByUserIdAndSongId(Long userId, Long songId);
    boolean existsByUserIdAndSongId(Long userId, Long songId);
    long countByUserId(Long userId);

    @Query("SELECT r FROM Rating r JOIN FETCH r.user JOIN FETCH r.song ORDER BY r.user.id")
    List<Rating> findAllWithUserAndSong();
}
