package com.carrierfraud.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private static final String ISSUER = "fraud-sentinel";
    private static final long CLOCK_SKEW_SECONDS = 60L;

    @Value("${security.jwt.secret}")
    private String secret;

    @Value("${security.jwt.expiration-ms:3600000}")
    private long expirationMs;

    private SecretKey signingKey;

    @PostConstruct
    void init() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("security.jwt.secret must be configured");
        }
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("security.jwt.secret must decode to at least 256 bits");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        log.info("JwtService initialized with HS256 signing key (issuer={})", ISSUER);
    }

    public String generateToken(String username) {
        return generateToken(username, new HashMap<>());
    }

    public String generateToken(String username, Map<String, Object> claims) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username must not be null or blank");
        }
        Map<String, Object> payload = claims == null ? new HashMap<>() : new HashMap<>(claims);
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .claims(payload)
                .subject(username)
                .issuer(ISSUER)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public boolean isTokenValid(String token, String username) {
        if (token == null || username == null) {
            return false;
        }
        String subject = extractUsername(token);
        return Objects.equals(subject, username) && !isExpired(token);
    }

    public boolean isExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(parseAllClaims(token));
    }

    private Claims parseAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(ISSUER)
                .clockSkewSeconds(CLOCK_SKEW_SECONDS)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
