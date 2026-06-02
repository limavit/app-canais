package com.example.iptvmanager.auth.dto;

import com.example.iptvmanager.user.dto.UserDTO;

public record AuthResponse(
        String tokenType,
        String accessToken,
        String refreshToken,
        long expiresIn,
        UserDTO user
) {
}
