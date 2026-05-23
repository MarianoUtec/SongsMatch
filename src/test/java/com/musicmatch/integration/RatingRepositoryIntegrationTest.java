package com.musicmatch.integration;

import com.musicmatch.entity.Rating;
import com.musicmatch.entity.Role;
import com.musicmatch.entity.Song;
import com.musicmatch.entity.User;
import com.musicmatch.repository.RatingRepository;
import com.musicmatch.repository.SongRepository;
import com.musicmatch.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("RatingRepository Integration Tests (PostgreSQL)")
class RatingRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("musicmatch_test")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.database-platform",
            () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Autowired private RatingRepository ratingRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private SongRepository songRepository;

    private User user1;
    private User user2;
    private Song song1;
    private Song song2;

    @BeforeEach
    void setUp() {
        ratingRepository.deleteAll();
        songRepository.deleteAll();
        userRepository.deleteAll();

        user1 = userRepository.save(User.builder()
            .name("Alice").email("alice@integration.com").password("pw")
            .role(Role.USER).isActive(true).build());
        user2 = userRepository.save(User.builder()
            .name("Bob").email("bob@integration.com").password("pw")
            .role(Role.USER).isActive(true).build());

        song1 = songRepository.save(Song.builder()
            .title("Song One").artist("Artist One").spotifyId("sp_one").build());
        song2 = songRepository.save(Song.builder()
            .title("Song Two").artist("Artist Two").spotifyId("sp_two").build());
    }

    @Test
    @DisplayName("shouldPersistAndRetrieveRatingInPostgreSQL")
    void shouldPersistAndRetrieveRatingInPostgreSQL() {
        Rating saved = ratingRepository.save(
            Rating.builder().user(user1).song(song1).score(5).build());

        Optional<Rating> found = ratingRepository.findByUserIdAndSongId(user1.getId(), song1.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getScore()).isEqualTo(5);
        assertThat(found.get().getId()).isEqualTo(saved.getId());
    }

    @Test
    @DisplayName("shouldEnforceUniqueConstraintWhenDuplicateUserSongRating")
    void shouldEnforceUniqueConstraintWhenDuplicateUserSongRating() {
        ratingRepository.save(Rating.builder().user(user1).song(song1).score(5).build());

        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> {
            ratingRepository.saveAndFlush(
                Rating.builder().user(user1).song(song1).score(3).build());
        });
    }

    @Test
    @DisplayName("shouldReturnAllRatingsWithFetchedEntitiesWhenFindAllWithUserAndSong")
    void shouldReturnAllRatingsWithFetchedEntitiesWhenFindAllWithUserAndSong() {
        ratingRepository.save(Rating.builder().user(user1).song(song1).score(5).build());
        ratingRepository.save(Rating.builder().user(user1).song(song2).score(3).build());
        ratingRepository.save(Rating.builder().user(user2).song(song1).score(4).build());

        List<Rating> ratings = ratingRepository.findAllWithUserAndSong();

        assertThat(ratings).hasSize(3);
        ratings.forEach(r -> {
            assertThat(r.getUser().getName()).isNotNull();
            assertThat(r.getSong().getTitle()).isNotNull();
        });
    }

    @Test
    @DisplayName("shouldCountCorrectlyWhenCountByUserId")
    void shouldCountCorrectlyWhenCountByUserId() {
        ratingRepository.save(Rating.builder().user(user1).song(song1).score(5).build());
        ratingRepository.save(Rating.builder().user(user1).song(song2).score(3).build());

        long count = ratingRepository.countByUserId(user1.getId());

        assertThat(count).isEqualTo(2);
        assertThat(ratingRepository.countByUserId(user2.getId())).isEqualTo(0);
    }

    @Test
    @DisplayName("shouldOrderRatingsByUserWhenFindAllWithUserAndSong")
    void shouldOrderRatingsByUserWhenFindAllWithUserAndSong() {
        ratingRepository.save(Rating.builder().user(user2).song(song1).score(4).build());
        ratingRepository.save(Rating.builder().user(user1).song(song1).score(5).build());

        List<Rating> ratings = ratingRepository.findAllWithUserAndSong();

        assertThat(ratings.get(0).getUser().getId()).isLessThan(ratings.get(1).getUser().getId());
    }
}
