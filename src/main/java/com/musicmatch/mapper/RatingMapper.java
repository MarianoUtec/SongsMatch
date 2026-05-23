package com.musicmatch.mapper;
import com.musicmatch.dto.response.RatingResponse;
import com.musicmatch.entity.Rating;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
@Mapper(componentModel = "spring", uses = {SongMapper.class})
public interface RatingMapper {
    @Mapping(source = "user.id", target = "userId")
    RatingResponse toResponse(Rating rating);
}
