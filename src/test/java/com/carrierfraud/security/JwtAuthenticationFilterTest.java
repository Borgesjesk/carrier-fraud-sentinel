package com.carrierfraud.security;

import com.carrierfraud.config.CookieProperties;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final String COOKIE_NAME = "FS_SESSION";
    private static final String USERNAME = "alice";
    private static final String VALID_TOKEN = "valid.jwt.token";

    @Mock private JwtService jwtService;
    @Mock private CookieProperties cookieProperties;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain chain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        lenient().when(cookieProperties.name()).thenReturn(COOKIE_NAME);
        filter = new JwtAuthenticationFilter(jwtService, cookieProperties);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Cookie cookie(String name, String value) {
        return new Cookie(name, value);
    }

    private void stubCookies(Cookie... cookies) {
        when(request.getCookies()).thenReturn(cookies);
    }

    @Test
    void validCookie_populatesSecurityContext() throws Exception {
        stubCookies(cookie(cookieProperties.name(), VALID_TOKEN));
        when(jwtService.extractUsername(VALID_TOKEN)).thenReturn(USERNAME);
        when(jwtService.isTokenValid(VALID_TOKEN, USERNAME)).thenReturn(true);
        when(jwtService.extractClaim(anyString(), any())).thenReturn("ADMIN");

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo(USERNAME);
    }

    @Test
    void noCookies_chainContinuesWithoutAuth() throws Exception {
        when(request.getCookies()).thenReturn(null);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void wrongCookieName_chainContinuesWithoutAuth() throws Exception {
        stubCookies(cookie("OTHER_COOKIE", VALID_TOKEN));

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void emptyCookieValue_chainContinuesWithoutAuth() throws Exception {
        stubCookies(cookie(cookieProperties.name(), ""));

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void expiredJwtInCookie_chainContinuesWithoutAuth() throws Exception {
        stubCookies(cookie(cookieProperties.name(), VALID_TOKEN));
        when(jwtService.extractUsername(VALID_TOKEN)).thenThrow(new JwtException("expired"));

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void malformedJwtInCookie_doesNotLeakException() {
        stubCookies(cookie(cookieProperties.name(), "garbage"));
        when(jwtService.extractUsername("garbage")).thenThrow(new MalformedJwtException("bad"));

        assertThatCode(() -> filter.doFilterInternal(request, response, chain)).doesNotThrowAnyException();
    }
}
