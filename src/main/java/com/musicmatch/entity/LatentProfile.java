package com.musicmatch.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "latent_profiles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LatentProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // SVD coordinates (k=3 dimensions)
    @Column(name = "coord_x")
    private Double coordX;

    @Column(name = "coord_y")
    private Double coordY;

    @Column(name = "coord_z")
    private Double coordZ;

    @Column(name = "closest_user_id")
    private Long closestUserId;

    @Column(name = "compatibility_score")
    private Double compatibilityScore;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
