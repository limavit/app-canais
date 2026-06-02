package com.example.iptvmanager.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.example.iptvmanager.auth.dto.AuthResponse;
import com.example.iptvmanager.auth.dto.RegisterRequest;
import com.example.iptvmanager.auth.service.AuthService;
import com.example.iptvmanager.exception.BadRequestException;
import com.example.iptvmanager.user.repository.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void shouldStorePasswordWithBCrypt() {
        authService.register(new RegisterRequest("BCrypt User", "bcrypt@example.com", "123456"));

        var user = userRepository.findByEmailIgnoreCase("bcrypt@example.com").orElseThrow();

        assertThat(user.getPassword()).isNotEqualTo("123456");
        assertThat(passwordEncoder.matches("123456", user.getPassword())).isTrue();
    }

    @Test
    void shouldRefreshToken() {
        AuthResponse response = authService.register(new RegisterRequest("Refresh User", "refresh@example.com", "123456"));

        AuthResponse refreshed = authService.refresh(new com.example.iptvmanager.auth.dto.RefreshTokenRequest(response.refreshToken()));

        assertThat(refreshed.accessToken()).isNotBlank();
        assertThat(refreshed.refreshToken()).isNotBlank();
        assertThat(refreshed.user().email()).isEqualTo("refresh@example.com");
    }

    @Test
    void shouldRejectInvalidRefreshToken() {
        assertThatThrownBy(() -> authService.refresh(new com.example.iptvmanager.auth.dto.RefreshTokenRequest("invalid")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Refresh token invalido");
    }
}
