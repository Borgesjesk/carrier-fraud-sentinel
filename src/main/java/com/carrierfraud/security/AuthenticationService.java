package com.carrierfraud.security;

import com.carrierfraud.domain.User;
import com.carrierfraud.infrastructure.UserRepository;
import com.carrierfraud.security.dto.AuthResponse;
import com.carrierfraud.security.dto.LoginRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);
    private static final String ROLE_CLAIM = "role";

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final com.carrierfraud.infrastructure.RefreshTokenRepository refreshTokenRepository;

    public AuthenticationService(AuthenticationManager authenticationManager,
                                 JwtService jwtService,
                                 UserRepository userRepository,
                                 com.carrierfraud.infrastructure.RefreshTokenRepository refreshTokenRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public LoginResult login(LoginRequest request, String remoteAddr) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );

            User user = loadAuthenticatedUser(authentication.getName());
            String role = user.getRole().name();
            String token = jwtService.generateToken(user.getUsername(), Map.of(ROLE_CLAIM, role));

            log.info("Successful login: username={} role={} remote={}",
                    user.getUsername(), role, remoteAddr);

            return new LoginResult(token, new AuthResponse(user.getUsername(), role));

        } catch (AuthenticationException ex) {
            log.warn("Failed login attempt: username={} remote={} reason={}",
                    request.username(), remoteAddr, ex.getClass().getSimpleName());
            throw ex;
        }
    }

    public String refreshAccessToken(String refreshTokenId) {
        if (refreshTokenId == null || refreshTokenId.isBlank()) {
            throw new org.springframework.security.core.AuthenticationException("Refresh token missing") {};
        }

        com.carrierfraud.domain.RefreshToken token = refreshTokenRepository.findByTokenId(refreshTokenId)
                .orElseThrow(() -> new org.springframework.security.core.AuthenticationException("Refresh token not found") {});

        if (!token.isValid()) {
            throw new org.springframework.security.core.AuthenticationException("Refresh token expired or revoked") {};
        }

        com.carrierfraud.domain.User user = userRepository.findByUsername(token.getUsername())
                .orElseThrow(() -> new org.springframework.security.core.AuthenticationException("User not found") {});

        return jwtService.generateToken(user.getUsername(), java.util.Map.of("role", user.getRole().name()));
    }

    private User loadAuthenticatedUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user missing from repository"));

    }
}
