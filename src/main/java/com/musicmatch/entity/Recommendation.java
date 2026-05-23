package com.musicmatch.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "recommendations", indexes = {
    @Index(name = "idx_recommendation_user", columnList = "user_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Recommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "recommendation_songs",
        joinColumns = @JoinColumn(name = "recommendation_id"),
        inverseJoinColumns = @JoinColumn(name = "song_id")
    )
    @Builder.Default
    private List<Song> songs = new ArrayList<>();

    @Column(name = "based_on_user_id")
    private Long basedOnUserId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
