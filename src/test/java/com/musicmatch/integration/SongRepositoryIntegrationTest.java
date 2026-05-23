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
@DisplayName("SongRepository Integration Tests (PostgreSQL)")
class SongRepositoryIntegrationTest {

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

    @Autowired private SongRepository songRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RatingRepository ratingRepository;

    private User user;
    private Song song1;
    private Song song2;
    private Song song3;

    @BeforeEach
    void setUp() {
        ratingRepository.deleteAll();
        songRepository.deleteAll();
        userRepository.deleteAll();

        user = userRepository.save(User.builder()
            .name("Alice").email("alice@song-pg.com")
            .password("pw").role(Role.USER).isActive(true).build());

        song1 = songRepository.save(Song.builder()
            .title("Pyramids").artist("Frank Ocean").spotifyId("sp1").build());
        song2 = songRepository.save(Song.builder()
            .title("Retrograde").artist("James Blake").spotifyId("sp2").build());
        song3 = songRepository.save(Song.builder()
            .title("PRIDE.").artist("Kendrick Lamar").spotifyId("sp3").build());
    }

    @Test
    @DisplayName("shouldFindBySpotifyIdWhenSongExistsInPostgreSQL")
    void shouldFindBySpotifyIdWhenSongExistsInPostgreSQL() {
        Optional<Song> result = songRepository.findBySpotifyId("sp1");

        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("Pyramids");
    }

    @Test
    @DisplayName("shouldEnforceUniqueSpotifyIdWhenDuplicateInserted")
    void shouldEnforceUniqueSpotifyIdWhenDuplicateInserted() {
        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () ->
            songRepository.saveAndFlush(Song.builder()
                .title("Copy").artist("Copy Artist").spotifyId("sp1").build())
        );
    }

    @Test
    @DisplayName("shouldFindSongsNotRatedByUserWhenUserRatedSomeSongs")
    void shouldFindSongsNotRatedByUserWhenUserRatedSomeSongs() {
        ratingRepository.save(Rating.builder().user(user).song(song1).score(5).build());

        List<Song> unrated = songRepository.findSongsNotRatedByUser(user.getId());

        assertThat(unrated).hasSize(2);
        assertThat(unrated).noneMatch(s -> s.getId().equals(song1.getId()));
    }

    @Test
    @DisplayName("shouldSearchByTitleCaseInsensitiveWhenQueryProvided")
    void shouldSearchByTitleCaseInsensitiveWhenQueryProvided() {
        List<Song> result = songRepository.findByTitleContainingIgnoreCase("pyr");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Pyramids");
    }

    @Test
    @DisplayName("shouldSearchByArtistCaseInsensitiveWhenQueryProvided")
    void shouldSearchByArtistCaseInsensitiveWhenQueryProvided() {
        List<Song> result = songRepository.findByArtistContainingIgnoreCase("james");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getArtist()).isEqualTo("James Blake");
    }

    @Test
    @DisplayName("shouldReturnEmptyListWhenAllSongsAlreadyRated")
    void shouldReturnEmptyListWhenAllSongsAlreadyRated() {
        ratingRepository.save(Rating.builder().user(user).song(song1).score(5).build());
        ratingRepository.save(Rating.builder().user(user).song(song2).score(4).build());
        ratingRepository.save(Rating.builder().user(user).song(song3).score(3).build());

        List<Song> unrated = songRepository.findSongsNotRatedByUser(user.getId());

        assertThat(unrated).isEmpty();
    }
}
