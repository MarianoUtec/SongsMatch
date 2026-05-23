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
@DisplayName("SongRepository Tests")
class SongRepositoryTest {

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

        song1 = songRepository.save(Song.builder()
            .title("Bohemian Rhapsody").artist("Queen").spotifyId("sp_bohemian").build());
        song2 = songRepository.save(Song.builder()
            .title("Stairway to Heaven").artist("Led Zeppelin").spotifyId("sp_stairway").build());
        song3 = songRepository.save(Song.builder()
            .title("Hotel California").artist("Eagles").spotifyId("sp_hotel").build());

        user = userRepository.save(User.builder()
            .name("Alice").email("alice@test.com").password("pw").role(Role.USER).isActive(true).build());

        ratingRepository.save(Rating.builder().user(user).song(song1).score(5).build());
    }

    @Test
    @DisplayName("shouldReturnSongWhenFindByExistingSpotifyId")
    void shouldReturnSongWhenFindByExistingSpotifyId() {
        Optional<Song> result = songRepository.findBySpotifyId("sp_bohemian");

        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("Bohemian Rhapsody");
    }

    @Test
    @DisplayName("shouldReturnEmptyWhenFindByNonExistingSpotifyId")
    void shouldReturnEmptyWhenFindByNonExistingSpotifyId() {
        Optional<Song> result = songRepository.findBySpotifyId("non_existing");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("shouldReturnTrueWhenExistsByRegisteredSpotifyId")
    void shouldReturnTrueWhenExistsByRegisteredSpotifyId() {
        boolean exists = songRepository.existsBySpotifyId("sp_bohemian");

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("shouldReturnSongsWhenFindByArtistContainingIgnoreCase")
    void shouldReturnSongsWhenFindByArtistContainingIgnoreCase() {
        List<Song> songs = songRepository.findByArtistContainingIgnoreCase("queen");

        assertThat(songs).hasSize(1);
        assertThat(songs.get(0).getTitle()).isEqualTo("Bohemian Rhapsody");
    }

    @Test
    @DisplayName("shouldReturnSongsWhenFindByTitleContainingIgnoreCase")
    void shouldReturnSongsWhenFindByTitleContainingIgnoreCase() {
        List<Song> songs = songRepository.findByTitleContainingIgnoreCase("hotel");

        assertThat(songs).hasSize(1);
        assertThat(songs.get(0).getArtist()).isEqualTo("Eagles");
    }

    @Test
    @DisplayName("shouldReturnUnratedSongsWhenFindSongsNotRatedByUser")
    void shouldReturnUnratedSongsWhenFindSongsNotRatedByUser() {
        List<Song> unrated = songRepository.findSongsNotRatedByUser(user.getId());

        assertThat(unrated).hasSize(2);
        assertThat(unrated).extracting(Song::getTitle)
            .containsExactlyInAnyOrder("Stairway to Heaven", "Hotel California");
    }

    @Test
    @DisplayName("shouldReturnAllSongsWhenUserHasNoRatings")
    void shouldReturnAllSongsWhenUserHasNoRatings() {
        User newUser = userRepository.save(User.builder()
            .name("New").email("new@test.com").password("pw").role(Role.USER).isActive(true).build());

        List<Song> unrated = songRepository.findSongsNotRatedByUser(newUser.getId());

        assertThat(unrated).hasSize(3);
    }

    @Test
    @DisplayName("shouldReturnMultipleSongsWhenSearchingByPartialArtistName")
    void shouldReturnMultipleSongsWhenSearchingByPartialArtistName() {
        songRepository.save(Song.builder()
            .title("Another One Bites the Dust").artist("Queen").spotifyId("sp_another").build());

        List<Song> songs = songRepository.findByArtistContainingIgnoreCase("Queen");

        assertThat(songs).hasSize(2);
    }
}
