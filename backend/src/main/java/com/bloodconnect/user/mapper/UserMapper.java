package com.bloodconnect.user.mapper;

import com.bloodconnect.user.dto.UserResponse;
import com.bloodconnect.user.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);
}
