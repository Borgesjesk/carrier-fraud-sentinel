package com.carrierfraud.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MissingClaimException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String VALID_SECRET = Base64.getEncoder().encodeToString(new byte[64]);
    private static final long ONE_HOUR_MS = 3_600_000L;
    private static final String USERNAME = "alice";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", VALID_SECRET);
        ReflectionTestUtils.setField(jwtService, "expirationMs", ONE_HOUR_MS);
        jwtService.init();
    }

    @Test
    void generateToken_returnsValidJwt() {
        String token = jwtService.generateToken(USERNAME);

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void generateToken_includesCustomClaims() {
        String token = jwtService.generateToken(USERNAME, Map.of("role", "ADMIN"));

        String role = jwtService.extractClaim(token, c -> (String) c.get("role"));
        assertThat(role).isEqualTo("ADMIN");
    }

    @Test
    void generateToken_throwsOnNullUsername() {
        assertThatThrownBy(() -> jwtService.generateToken(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void generateToken_throwsOnBlankUsername() {
        assertThatThrownBy(() -> jwtService.generateToken("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void extractUsername_returnsSubject() {
        String token = jwtService.generateToken(USERNAME);

        assertThat(jwtService.extractUsername(token)).isEqualTo(USERNAME);
    }

    @Test
    void isTokenValid_returnsTrueForMatchingUsername() {
        String token = jwtService.generateToken(USERNAME);

        assertThat(jwtService.isTokenValid(token, USERNAME)).isTrue();
    }

    @Test
    void isTokenValid_returnsFalseForMismatchedUsername() {
        String token = jwtService.generateToken(USERNAME);

        assertThat(jwtService.isTokenValid(token, "mallory")).isFalse();
    }

    @Test
    void isTokenValid_returnsFalseForNullToken() {
        assertThat(jwtService.isTokenValid(null, USERNAME)).isFalse();
    }

    @Test
    void isTokenValid_returnsFalseForNullUsername() {
        String token = jwtService.generateToken(USERNAME);

        assertThat(jwtService.isTokenValid(token, null)).isFalse();
    }

    @Test
    void parseAllClaims_throwsOnExpiredToken() {
        JwtService shortLivedService = new JwtService();
        ReflectionTestUtils.setField(shortLivedService, "secret", VALID_SECRET);
        ReflectionTestUtils.setField(shortLivedService, "expirationMs", -1L);
        shortLivedService.init();

        String expired = shortLivedService.generateToken(USERNAME);

        assertThatThrownBy(() -> shortLivedService.extractUsername(expired))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void parseAllClaims_throwsOnTamperedSignature() {
        String token = jwtService.generateToken(USERNAME);
        String tampered = token.substring(0, token.length() - 4) + "XXXX";

        assertThatThrownBy(() -> jwtService.extractUsername(tampered))
                .isInstanceOf(SignatureException.class);
    }

    @Test
    void parseAllClaims_throwsOnWrongIssuer() {
        JwtService rogue = new JwtService();
        ReflectionTestUtils.setField(rogue, "secret", VALID_SECRET);
        ReflectionTestUtils.setField(rogue, "expirationMs", ONE_HOUR_MS);
        rogue.init();

        ReflectionTestUtils.setField(rogue, "signingKey",
                ReflectionTestUtils.getField(jwtService, "signingKey"));

        String rogueToken = io.jsonwebtoken.Jwts.builder()
                .subject(USERNAME)
                .issuer("evil-issuer")
                .expiration(new java.util.Date(System.currentTimeMillis() + ONE_HOUR_MS))
                .signWith((javax.crypto.SecretKey) ReflectionTestUtils.getField(jwtService, "signingKey"))
                .compact();

        assertThatThrownBy(() -> jwtService.extractUsername(rogueToken))
                .isInstanceOf(MissingClaimException.class);
    }

    @Test
    void init_throwsOnBlankSecret() {
        JwtService service = new JwtService();
        ReflectionTestUtils.setField(service, "secret", "  ");

        assertThatThrownBy(service::init)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must be configured");
    }

    @Test
    void init_throwsOnNullSecret() {
        JwtService service = new JwtService();
        ReflectionTestUtils.setField(service, "secret", null);

        assertThatThrownBy(service::init)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void init_throwsOnSecretShorterThan256Bits() {
        JwtService service = new JwtService();
        ReflectionTestUtils.setField(service, "secret",
                Base64.getEncoder().encodeToString(new byte[16]));

        assertThatThrownBy(service::init)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("256 bits");
    }
}