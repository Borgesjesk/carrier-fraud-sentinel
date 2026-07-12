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
    private final MfaService mfaService;

    public AuthenticationService(AuthenticationManager authenticationManager,
                                 JwtService jwtService,
                                 UserRepository userRepository,
                                 com.carrierfraud.infrastructure.RefreshTokenRepository refreshTokenRepository,
                                 MfaService mfaService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.mfaService = mfaService;
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

            user.addKnownIp(remoteAddr);
            userRepository.save(user);

            if (user.isMfaEnabled()) {
                log.info("MFA challenge issued: username={}", user.getUsername());
                return LoginResult.mfaChallenge();
            }
            return LoginResult.authenticated(token, new AuthResponse(user.getUsername(), role));

        } catch (AuthenticationException ex) {
            log.warn("Failed login attempt: username={} remote={} reason={}",
                    request.username(), remoteAddr, ex.getClass().getSimpleName());
            throw ex;
        }
    }

    public LoginResult loginMfa(LoginRequest request, int totpCode, String remoteAddr) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
            User user = loadAuthenticatedUser(authentication.getName());

            if (!user.isMfaEnabled()) {
                throw new AuthenticationException("MFA not enabled for user") {
                };
            }

            if (!mfaService.verifyCode(user.getUsername(), totpCode)) {
                log.warn("Invalid MFA code: username={} remote={}", user.getUsername(), remoteAddr);
                throw new AuthenticationException("Invalid MFA code") {
                };
            }

            String role = user.getRole().name();
            String token = jwtService.generateToken(user.getUsername(), Map.of(ROLE_CLAIM, role));
            log.info("Successful MFA login: username={} role={} remote={}",
                    user.getUsername(), role, remoteAddr);
            user.addKnownIp(remoteAddr);
            userRepository.save(user);
            return LoginResult.authenticated(token, new AuthResponse(user.getUsername(), role));
        } catch (AuthenticationException ex) {
            log.warn("Failed MFA login: username={} remote={} reason={}",
                    request.username(), remoteAddr, ex.getClass().getSimpleName());
            throw ex;
        }
    }

    public RefreshResult refreshAccessToken(String refreshTokenId) {
        if (refreshTokenId == null || refreshTokenId.isBlank()) {
            throw new org.springframework.security.core.AuthenticationException("Refresh token missing") {
            };
        }
        com.carrierfraud.domain.RefreshToken token = refreshTokenRepository.findByTokenId(refreshTokenId)
                .orElseThrow(() -> new org.springframework.security.core.AuthenticationException("Refresh token not found") {
                });

        if (!token.isValid()) {
            if (token.isRevoked()) {
                log.warn("Refresh token reuse detected: username={} tokenId={}", token.getUsername(), refreshTokenId);
                refreshTokenRepository.findAll().stream()
                        .filter(t -> t.getUsername().equals(token.getUsername()) && !t.isRevoked())
                        .forEach(t -> {
                            t.revoke();
                            refreshTokenRepository.save(t);
                        });
            }
            throw new org.springframework.security.core.AuthenticationException("Refresh token expired or revoked") {
            };
        }

        com.carrierfraud.domain.User user = userRepository.findByUsername(token.getUsername())
                .orElseThrow(() -> new org.springframework.security.core.AuthenticationException("User not found") {
                });

        token.revoke();
        refreshTokenRepository.save(token);

        com.carrierfraud.domain.RefreshToken newToken = new com.carrierfraud.domain.RefreshToken(
                user.getUsername(),
                java.time.LocalDateTime.now().plusDays(7)
        );
        refreshTokenRepository.save(newToken);

        String accessToken = jwtService.generateToken(user.getUsername(), java.util.Map.of("role", user.getRole().name()));
        return new RefreshResult(accessToken, newToken.getTokenId());
    }

    public record RefreshResult(String accessToken, String refreshTokenId) {
    }

    private User loadAuthenticatedUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user missing from repository"));

    }
}
