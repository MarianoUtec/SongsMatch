package com.musicmatch.recommendation.domain;

import com.musicmatch.auth.domain.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "latent_profile_history", indexes = {
    @Index(name = "idx_lph_user", columnList = "user_id"),
    @Index(name = "idx_lph_recorded_at", columnList = "recorded_at")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LatentProfileHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "coord_x")
    private Double coordX;

    @Column(name = "coord_y")
    private Double coordY;

    @Column(name = "coord_z")
    private Double coordZ;

    @Column(name = "closest_user_id")
    private Long closestUserId;

    @Column(name = "closest_user_name", length = 100)
    private String closestUserName;

    @Column(name = "compatibility_score")
    private Double compatibilityScore;

    @Column(name = "ratings_count")
    private Integer ratingsCount;

    @CreationTimestamp
    @Column(name = "recorded_at", updatable = false)
    private LocalDateTime recordedAt;
}
