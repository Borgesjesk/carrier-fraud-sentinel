package com.carrierfraud.security;

import com.carrierfraud.config.CookieProperties;
import com.carrierfraud.security.dto.AuthResponse;
import com.carrierfraud.security.dto.LoginRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String USERNAME = "alice";
    private static final String VALID_PASSWORD = "correct-horse-battery";
    private static final String JWT = "jwt.token.here";
    private static final String COOKIE_NAME = "FS_SESSION";
    private static final String COOKIE_PATH = "/";
    private static final String COOKIE_SAME_SITE = "Strict";
    private static final long COOKIE_MAX_AGE = 3600L;

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private AuthenticationService authenticationService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean private CookieProperties cookieProperties;
    @MockitoBean private com.carrierfraud.infrastructure.RefreshTokenRepository refreshTokenRepository;
    @MockitoBean private PasswordResetService passwordResetService;

    @BeforeEach
    void setUp() {
        when(cookieProperties.name()).thenReturn(COOKIE_NAME);
        when(cookieProperties.secure()).thenReturn(true);
        when(cookieProperties.sameSite()).thenReturn(COOKIE_SAME_SITE);
        when(cookieProperties.maxAgeSeconds()).thenReturn(COOKIE_MAX_AGE);
        when(cookieProperties.path()).thenReturn(COOKIE_PATH);
    }

    @Test
    void login_setsAuthCookieOnValidCredentials() throws Exception {
        LoginResult result = new LoginResult(JWT, new AuthResponse(USERNAME, "ADMIN"));
        when(authenticationService.login(any(LoginRequest.class), anyString())).thenReturn(result);

        MvcResult mvcResult = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(USERNAME, VALID_PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andReturn();

        String setCookie = mvcResult.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertAll(
                () -> assertThat(setCookie).startsWith(COOKIE_NAME + "=" + JWT),
                () -> assertThat(setCookie).contains("HttpOnly"),
                () -> assertThat(setCookie).contains("Secure"),
                () -> assertThat(setCookie).contains("SameSite=" + COOKIE_SAME_SITE),
                () -> assertThat(setCookie).contains("Path=" + COOKIE_PATH),
                () -> assertThat(setCookie).contains("Max-Age=" + COOKIE_MAX_AGE)
        );
    }

    @Test
    void login_returnsUsernameAndRoleInBodyWithoutAccessToken() throws Exception {
        LoginResult result = new LoginResult(JWT, new AuthResponse(USERNAME, "ADMIN"));
        when(authenticationService.login(any(LoginRequest.class), anyString())).thenReturn(result);

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(USERNAME, VALID_PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(USERNAME))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.accessToken").doesNotExist());
    }

    @Test
    void login_returns401AndNoCookieOnBadCredentials() throws Exception {
        when(authenticationService.login(any(LoginRequest.class), anyString()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(USERNAME, "wrong-password-attempt"))))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    }

    @Test
    void login_returns400OnUsernameWithInvalidCharacters() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("alice<script>", VALID_PASSWORD))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_returns400OnShortPassword() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(USERNAME, "short"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void me_authenticated_returnsUsernameAndRole() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "admin", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        var response = new AuthController(authenticationService, cookieProperties, refreshTokenRepository, passwordResetService).me(auth);

        assertAll(
                () -> assertThat(response.getStatusCode().value()).isEqualTo(200),
                () -> assertThat(response.getBody().username()).isEqualTo("admin"),
                () -> assertThat(response.getBody().role()).isEqualTo("ADMIN"),
                () -> assertThat(response.getHeaders().get(HttpHeaders.SET_COOKIE)).isNull()
        );
    }

    @Test
    void me_analystRole_returnsAnalyst() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "alice", null, List.of(new SimpleGrantedAuthority("ROLE_ANALYST")));

        var response = new AuthController(authenticationService, cookieProperties, refreshTokenRepository, passwordResetService).me(auth);

        assertThat(response.getBody().role()).isEqualTo("ANALYST");
    }

    @Test
    void me_noRolePrefix_throwsAccessDeniedException() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "alice", null, List.of(new SimpleGrantedAuthority("not_a_role")));
        AuthController controller = new AuthController(authenticationService, cookieProperties, refreshTokenRepository, passwordResetService);

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () -> controller.me(auth));

        assertThat(ex.getMessage()).isEqualTo("No role assigned to authenticated user");
    }

    @Test
    void me_emptyAuthorities_throwsAccessDeniedException() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "alice", null, List.of());
        AuthController controller = new AuthController(authenticationService, cookieProperties, refreshTokenRepository, passwordResetService);

        assertThrows(AccessDeniedException.class, () -> controller.me(auth));
    }

    @Test
    void logout_returns204AndClearsCookie() throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isNoContent())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andReturn();

        String setCookie = mvcResult.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertAll(
                () -> assertThat(setCookie).contains(cookieProperties.name() + "="),
                () -> assertThat(setCookie).contains("Max-Age=0"),
                () -> assertThat(setCookie).contains("HttpOnly"),
                () -> assertThat(setCookie).contains("Path=" + COOKIE_PATH)
        );
    }

    @Test
    void logout_isIdempotent_returns204EvenWithoutCookie() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isNoContent());
    }

    @Test
    void logout_clearedCookieHasSamePathAsOriginal() throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isNoContent())
                .andReturn();

        String setCookie = mvcResult.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).contains("Path=" + COOKIE_PATH);
    }
}
