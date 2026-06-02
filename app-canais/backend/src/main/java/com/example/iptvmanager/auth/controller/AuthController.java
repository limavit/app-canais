package com.example.iptvmanager.auth.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.iptvmanager.auth.dto.AuthResponse;
import com.example.iptvmanager.auth.dto.LoginRequest;
import com.example.iptvmanager.auth.dto.RefreshTokenRequest;
import com.example.iptvmanager.auth.dto.RegisterRequest;
import com.example.iptvmanager.auth.security.UserPrincipal;
import com.example.iptvmanager.auth.service.AuthService;
import com.example.iptvmanager.user.dto.UserDTO;
import com.example.iptvmanager.user.mapper.UserMapper;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request);
    }

    @PostMapping("/logout")
    public void logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
    }

    @GetMapping("/me")
    public UserDTO me(@AuthenticationPrincipal UserPrincipal principal) {
        return UserMapper.toDTO(principal.getUser());
    }
}
