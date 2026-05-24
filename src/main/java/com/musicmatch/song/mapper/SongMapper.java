package com.musicmatch.song.mapper;
import com.musicmatch.song.dto.response.SongResponse;
import com.musicmatch.song.domain.Song;
import org.mapstruct.Mapper;
@Mapper(componentModel = "spring")
public interface SongMapper {
    SongResponse toResponse(Song song);
}
