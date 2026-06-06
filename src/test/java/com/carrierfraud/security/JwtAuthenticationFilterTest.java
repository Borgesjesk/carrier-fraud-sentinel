package com.carrierfraud.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final String USERNAME = "alice";
    private static final String VALID_TOKEN = "valid.jwt.token";
    private static final String BEARER_HEADER = "Bearer " + VALID_TOKEN;

    @Mock private JwtService jwtService;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain chain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void noAuthHeader_continuesChainWithoutAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void nonBearerHeader_continuesChainWithoutAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic abc123");

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(jwtService, never()).extractUsername(anyString());
    }

    @Test
    void emptyBearerToken_continuesChainWithoutAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer    ");

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(jwtService, never()).extractUsername(anyString());
    }

    @Test
    void validTokenWithKnownRole_setsAuthenticationWithAuthority() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(BEARER_HEADER);
        when(jwtService.extractUsername(VALID_TOKEN)).thenReturn(USERNAME);
        when(jwtService.isTokenValid(VALID_TOKEN, USERNAME)).thenReturn(true);
        when(jwtService.extractClaim(anyString(), any())).thenReturn("ADMIN");

        filter.doFilterInternal(request, response, chain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo(USERNAME);
        assertThat(auth.getAuthorities()).extracting("authority").contains("ROLE_ADMIN");
    }

    @Test
    void validTokenWithUnknownRole_setsAuthenticationWithoutAuthority() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(BEARER_HEADER);
        when(jwtService.extractUsername(VALID_TOKEN)).thenReturn(USERNAME);
        when(jwtService.isTokenValid(VALID_TOKEN, USERNAME)).thenReturn(true);
        when(jwtService.extractClaim(anyString(), any())).thenReturn("HACKER_ROLE");

        filter.doFilterInternal(request, response, chain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities()).isEmpty();
    }

    @Test
    void invalidToken_clearsContextAndContinuesChain() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(BEARER_HEADER);
        when(jwtService.extractUsername(VALID_TOKEN)).thenThrow(new MalformedJwtException("bad"));

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void tokenValidationFails_doesNotSetAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(BEARER_HEADER);
        when(jwtService.extractUsername(VALID_TOKEN)).thenReturn(USERNAME);
        when(jwtService.isTokenValid(VALID_TOKEN, USERNAME)).thenReturn(false);

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain, times(1)).doFilter(request, response);
    }

    @Test
    void jwtExceptionLogsUserAgent() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(BEARER_HEADER);
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(jwtService.extractUsername(VALID_TOKEN)).thenThrow(new JwtException("bad"));

        filter.doFilterInternal(request, response, chain);

        verify(request).getHeader("User-Agent");
        verify(chain).doFilter(request, response);
    }
}