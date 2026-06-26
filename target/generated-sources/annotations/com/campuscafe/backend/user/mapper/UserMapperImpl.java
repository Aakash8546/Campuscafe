package com.campuscafe.backend.user.mapper;

import com.campuscafe.backend.domain.user.Role;
import com.campuscafe.backend.domain.user.User;
import com.campuscafe.backend.user.dto.UserResponse;
import com.campuscafe.backend.user.dto.UserSummaryResponse;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-26T17:04:45+0530",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.12 (Oracle Corporation)"
)
@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public UserResponse toResponse(User user) {
        if ( user == null ) {
            return null;
        }

        UserResponse.UserResponseBuilder userResponse = UserResponse.builder();

        userResponse.role( userRoleName( user ) );
        userResponse.id( user.getId() );
        userResponse.name( user.getName() );
        userResponse.email( user.getEmail() );
        userResponse.phone( user.getPhone() );
        userResponse.active( user.getActive() );
        userResponse.createdAt( user.getCreatedAt() );
        userResponse.updatedAt( user.getUpdatedAt() );

        return userResponse.build();
    }

    @Override
    public UserSummaryResponse toSummaryResponse(User user) {
        if ( user == null ) {
            return null;
        }

        UserSummaryResponse.UserSummaryResponseBuilder userSummaryResponse = UserSummaryResponse.builder();

        userSummaryResponse.role( userRoleName( user ) );
        userSummaryResponse.id( user.getId() );
        userSummaryResponse.name( user.getName() );
        userSummaryResponse.email( user.getEmail() );
        userSummaryResponse.active( user.getActive() );

        return userSummaryResponse.build();
    }

    private String userRoleName(User user) {
        if ( user == null ) {
            return null;
        }
        Role role = user.getRole();
        if ( role == null ) {
            return null;
        }
        String name = role.getName();
        if ( name == null ) {
            return null;
        }
        return name;
    }
}
