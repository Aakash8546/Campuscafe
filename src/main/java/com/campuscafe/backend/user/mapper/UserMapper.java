package com.campuscafe.backend.user.mapper;

import com.campuscafe.backend.domain.user.User;
import com.campuscafe.backend.user.dto.UserResponse;
import com.campuscafe.backend.user.dto.UserSummaryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "role", source = "role.name")
    UserResponse toResponse(User user);

    @Mapping(target = "role", source = "role.name")
    UserSummaryResponse toSummaryResponse(User user);
}
