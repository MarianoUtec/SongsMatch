package com.musicmatch.mapper;

import com.musicmatch.dto.response.UserResponse;
import com.musicmatch.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toResponse(User user);
}
