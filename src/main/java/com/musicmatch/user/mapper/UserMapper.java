package com.musicmatch.user.mapper;

import com.musicmatch.user.dto.response.UserResponse;
import com.musicmatch.auth.domain.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toResponse(User user);
}
