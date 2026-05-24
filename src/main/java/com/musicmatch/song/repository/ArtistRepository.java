package com.musicmatch.song.repository;

import com.musicmatch.song.domain.Artist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ArtistRepository extends JpaRepository<Artist, Long> {
    Optional<Artist> findByName(String name);
    Optional<Artist> findBySpotifyId(String spotifyId);
    boolean existsBySpotifyId(String spotifyId);
}
