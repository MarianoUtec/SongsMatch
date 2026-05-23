package com.musicmatch.repository;

import com.musicmatch.entity.Song;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SongRepository extends JpaRepository<Song, Long> {
    Optional<Song> findBySpotifyId(String spotifyId);
    boolean existsBySpotifyId(String spotifyId);
    List<Song> findByArtistContainingIgnoreCase(String artist);
    List<Song> findByTitleContainingIgnoreCase(String title);

    @Query("SELECT s FROM Song s WHERE s.id NOT IN " +
           "(SELECT r.song.id FROM Rating r WHERE r.user.id = :userId)")
    List<Song> findSongsNotRatedByUser(Long userId);
}
