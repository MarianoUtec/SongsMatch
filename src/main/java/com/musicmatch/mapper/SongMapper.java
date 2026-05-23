package com.musicmatch.mapper;
import com.musicmatch.dto.response.SongResponse;
import com.musicmatch.entity.Song;
import org.mapstruct.Mapper;
@Mapper(componentModel = "spring")
public interface SongMapper {
    SongResponse toResponse(Song song);
}
