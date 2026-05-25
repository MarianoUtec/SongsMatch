package com.musicmatch.song.domain;

import com.musicmatch.recommendation.domain.Rating;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "songs", indexes = {
    @Index(name = "idx_song_spotify_id", columnList = "spotify_id", unique = true),
    @Index(name = "idx_song_musicbrainz_id", columnList = "musicbrainz_id", unique = true)
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Song {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 200)
    private String title;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String artist;

    @Column(name = "spotify_id", unique = true, length = 100)
    private String spotifyId;

    @Column(name = "musicbrainz_id", unique = true, length = 100)
    private String musicbrainzId;

    @Column(name = "album_name", length = 200)
    private String albumName;

    @Column(name = "cover_url", length = 500)
    private String coverUrl;

    @Column(name = "preview_url", length = 500)
    private String previewUrl;

    @Column(name = "duration_ms")
    private Integer durationMs;

    // Audio features from Spotify
    @Column(name = "danceability")
    private Double danceability;

    @Column(name = "energy")
    private Double energy;

    @Column(name = "valence")
    private Double valence;

    @Column(name = "tempo")
    private Double tempo;

    @Column(name = "acousticness")
    private Double acousticness;

    // Relaciones
    @OneToMany(mappedBy = "song", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Rating> ratings = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id")
    private Artist artistEntity;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "song_genres",
        joinColumns = @JoinColumn(name = "song_id"),
        inverseJoinColumns = @JoinColumn(name = "genre_id")
    )
    @Builder.Default
    private List<Genre> genres = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
