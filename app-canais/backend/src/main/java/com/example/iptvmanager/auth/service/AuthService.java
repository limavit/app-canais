package com.example.iptvmanager.auth.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.iptvmanager.auth.dto.AuthResponse;
import com.example.iptvmanager.auth.dto.LoginRequest;
import com.example.iptvmanager.auth.dto.RefreshTokenRequest;
import com.example.iptvmanager.auth.dto.RegisterRequest;
import com.example.iptvmanager.auth.entity.RefreshToken;
import com.example.iptvmanager.auth.security.UserPrincipal;
import com.example.iptvmanager.exception.BadRequestException;
import com.example.iptvmanager.user.entity.User;
import com.example.iptvmanager.user.entity.UserRole;
import com.example.iptvmanager.user.mapper.UserMapper;
import com.example.iptvmanager.user.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            RefreshTokenService refreshTokenService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BadRequestException("Email ja cadastrado");
        }

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.USER);
        user.setActive(true);

        User saved = userRepository.save(user);
        return response(saved);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                    request.email(),
                    request.password()
            ));
        } catch (AuthenticationException ex) {
            throw new BadRequestException("Email ou senha invalidos");
        }

        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new BadRequestException("Email ou senha invalidos"));

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new BadRequestException("Usuario inativo");
        }

        return response(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenService.validate(request.refreshToken());
        return response(refreshToken.getUser());
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        refreshTokenService.revoke(request.refreshToken());
    }

    private AuthResponse response(User user) {
        UserPrincipal principal = new UserPrincipal(user);
        String accessToken = jwtService.generateToken(principal);
        RefreshToken refreshToken = refreshTokenService.create(user);

        return new AuthResponse(
                "Bearer",
                accessToken,
                refreshToken.getToken(),
                jwtService.getExpirationMs(),
                UserMapper.toDTO(user)
        );
    }
}
