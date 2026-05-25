package com.musicmatch.config;

import com.musicmatch.song.domain.Song;
import com.musicmatch.song.repository.SongRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Objects;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final SongRepository songRepository;

    @Bean
    public CommandLineRunner initSongs() {
        return args -> {
            if (songRepository.count() > 0) return;

            List<Song> songs = List.of(
                Song.builder().title("Pyramids").artist("Frank Ocean")
                    .albumName("channel ORANGE").spotifyId("spotify:track:pyramids").build(),
                Song.builder().title("Pyramid Song").artist("Radiohead")
                    .albumName("Amnesiac").spotifyId("spotify:track:pyramidsong").build(),
                Song.builder().title("PRIDE.").artist("Kendrick Lamar")
                    .albumName("DAMN.").spotifyId("spotify:track:pride").build(),
                Song.builder().title("Apocalypse").artist("Cigarettes After Sex")
                    .albumName("Cigarettes After Sex").spotifyId("spotify:track:apocalypse").build(),
                Song.builder().title("Sand Dollars").artist("Unknown Mortal Orchestra")
                    .albumName("Multi-Love").spotifyId("spotify:track:sanddollars").build(),
                Song.builder().title("Retrograde").artist("James Blake")
                    .albumName("Overgrown").spotifyId("spotify:track:retrograde").build(),
                Song.builder().title("Cómo Me Quieres").artist("Kevin Kaarl")
                    .albumName("Cómo Me Quieres").spotifyId("spotify:track:comomeq").build(),
                Song.builder().title("The Wolves").artist("Phosphorescent")
                    .albumName("Muchacho").spotifyId("spotify:track:thewolves").build(),
                Song.builder().title("Joga").artist("Björk")
                    .albumName("Homogenic").spotifyId("spotify:track:joga").build(),
                Song.builder().title("White Ferrari").artist("Frank Ocean")
                    .albumName("Blonde").spotifyId("spotify:track:whiteferrari").build()
            );

            songRepository.saveAll(Objects.requireNonNull(songs));
            log.info("Initialized {} songs", songs.size());
        };
    }
}
