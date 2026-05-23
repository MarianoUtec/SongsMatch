package com.musicmatch.repository;

import com.musicmatch.entity.Rating;
import com.musicmatch.entity.Role;
import com.musicmatch.entity.Song;
import com.musicmatch.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("RatingRepository Tests")
class RatingRepositoryTest {

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
            .name("Alice").email("alice@test.com").password("pw").role(Role.USER).isActive(true).build());
        user2 = userRepository.save(User.builder()
            .name("Bob").email("bob@test.com").password("pw").role(Role.USER).isActive(true).build());

        song1 = songRepository.save(Song.builder()
            .title("Song A").artist("Artist A").spotifyId("sp1").build());
        song2 = songRepository.save(Song.builder()
            .title("Song B").artist("Artist B").spotifyId("sp2").build());

        ratingRepository.save(Rating.builder().user(user1).song(song1).score(5).build());
        ratingRepository.save(Rating.builder().user(user1).song(song2).score(3).build());
        ratingRepository.save(Rating.builder().user(user2).song(song1).score(4).build());
    }

    @Test
    @DisplayName("shouldReturnAllRatingsWhenFindByUserId")
    void shouldReturnAllRatingsWhenFindByUserId() {
        List<Rating> ratings = ratingRepository.findByUserId(user1.getId());

        assertThat(ratings).hasSize(2);
    }

    @Test
    @DisplayName("shouldReturnEmptyListWhenFindByUserIdWithNoRatings")
    void shouldReturnEmptyListWhenFindByUserIdWithNoRatings() {
        User newUser = userRepository.save(User.builder()
            .name("New").email("new@test.com").password("pw").role(Role.USER).isActive(true).build());

        List<Rating> ratings = ratingRepository.findByUserId(newUser.getId());

        assertThat(ratings).isEmpty();
    }

    @Test
    @DisplayName("shouldReturnRatingWhenFindByUserIdAndSongId")
    void shouldReturnRatingWhenFindByUserIdAndSongId() {
        Optional<Rating> rating = ratingRepository.findByUserIdAndSongId(user1.getId(), song1.getId());

        assertThat(rating).isPresent();
        assertThat(rating.get().getScore()).isEqualTo(5);
    }

    @Test
    @DisplayName("shouldReturnEmptyWhenFindByUserIdAndSongIdNotRated")
    void shouldReturnEmptyWhenFindByUserIdAndSongIdNotRated() {
        Optional<Rating> rating = ratingRepository.findByUserIdAndSongId(user2.getId(), song2.getId());

        assertThat(rating).isEmpty();
    }

    @Test
    @DisplayName("shouldReturnTrueWhenExistsByUserIdAndSongId")
    void shouldReturnTrueWhenExistsByUserIdAndSongId() {
        boolean exists = ratingRepository.existsByUserIdAndSongId(user1.getId(), song1.getId());

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("shouldReturnCorrectCountWhenCountByUserId")
    void shouldReturnCorrectCountWhenCountByUserId() {
        long count = ratingRepository.countByUserId(user1.getId());

        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("shouldReturnAllRatingsWithJoinWhenFindAllWithUserAndSong")
    void shouldReturnAllRatingsWithJoinWhenFindAllWithUserAndSong() {
        List<Rating> ratings = ratingRepository.findAllWithUserAndSong();

        assertThat(ratings).hasSize(3);
        ratings.forEach(r -> {
            assertThat(r.getUser()).isNotNull();
            assertThat(r.getSong()).isNotNull();
        });
    }

    @Test
    @DisplayName("shouldReturnRatingsBySongWhenFindBySongId")
    void shouldReturnRatingsBySongWhenFindBySongId() {
        List<Rating> ratings = ratingRepository.findBySongId(song1.getId());

        assertThat(ratings).hasSize(2);
    }
}
