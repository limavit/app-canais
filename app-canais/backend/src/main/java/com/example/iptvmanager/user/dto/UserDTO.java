package com.example.iptvmanager.user.dto;

import java.time.LocalDateTime;

import com.example.iptvmanager.user.entity.UserRole;

public record UserDTO(
        Long id,
        String name,
        String email,
        UserRole role,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
