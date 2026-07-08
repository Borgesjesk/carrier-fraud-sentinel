package com.carrierfraud.security;

import com.carrierfraud.domain.Role;
import com.carrierfraud.domain.User;
import com.carrierfraud.infrastructure.UserRepository;
import com.carrierfraud.security.dto.LoginRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    private static final String USERNAME = "alice";
    private static final String PASSWORD = "correct-horse-battery";
    private static final String REMOTE_ADDR = "192.168.1.10";
    private static final String EXPECTED_TOKEN = "generated.jwt.token";

    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;
    @Mock private UserRepository userRepository;
    @Mock private com.carrierfraud.infrastructure.RefreshTokenRepository refreshTokenRepository;

    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationService(authenticationManager, jwtService, userRepository, refreshTokenRepository);
    }

    @Test
    void login_returnsLoginResultOnValidCredentials() {
        LoginRequest request = new LoginRequest(USERNAME, PASSWORD);
        Authentication authentication = new UsernamePasswordAuthenticationToken(USERNAME, PASSWORD);

        User user = new User(USERNAME, "hash", Role.ADMIN, "test@test.local");

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
        when(jwtService.generateToken(anyString(), any())).thenReturn(EXPECTED_TOKEN);

        LoginResult result = authenticationService.login(request, REMOTE_ADDR);

        assertAll(
                () -> assertThat(result.token()).isEqualTo(EXPECTED_TOKEN),
                () -> assertThat(result.body().username()).isEqualTo(USERNAME),
                () -> assertThat(result.body().role()).isEqualTo("ADMIN")
        );
    }

    @Test
    void login_propagatesAuthenticationExceptionOnBadCredentials() {
        LoginRequest request = new LoginRequest(USERNAME, "wrong-password");
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authenticationService.login(request, REMOTE_ADDR))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_throwsWhenAuthenticatedUserMissingFromRepository() {
        LoginRequest request = new LoginRequest(USERNAME, PASSWORD);
        Authentication authentication = new UsernamePasswordAuthenticationToken(USERNAME, PASSWORD);

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.login(request, REMOTE_ADDR))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("missing from repository");
    }
}
