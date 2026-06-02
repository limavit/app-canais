package com.example.iptvmanager.user.mapper;

import com.example.iptvmanager.user.dto.UserDTO;
import com.example.iptvmanager.user.entity.User;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserDTO toDTO(User user) {
        return new UserDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getActive(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
