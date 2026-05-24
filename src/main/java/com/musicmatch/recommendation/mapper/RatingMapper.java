package com.musicmatch.recommendation.mapper;
import com.musicmatch.recommendation.dto.response.RatingResponse;
import com.musicmatch.recommendation.domain.Rating;
import com.musicmatch.song.mapper.SongMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
@Mapper(componentModel = "spring", uses = {SongMapper.class})
public interface RatingMapper {
    @Mapping(source = "user.id", target = "userId")
    RatingResponse toResponse(Rating rating);
}
