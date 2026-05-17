package com.carrierfraud.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${security.jwt.secret:bXktdmVyeS1zZWNyZXQta2V5LW11c3QtYmUtYXQtbGVhc3QtMjU2LWJpdHMtbG9uZw==}")
    private String secret;

    @Value("${security.jwt.expiration-ms:3600000}")
    private long expirationMs;

    public String generateToken(String username, Map<String, Object> claims) {
        Map<String, Object> payload = claims == null ? new HashMap<>() : new HashMap<>(claims);
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .claims(payload)
                .subject(username)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey())
                .compact();
    }

    public String generateToken(String username) {
        return generateToken(username, new HashMap<>());
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public boolean isTokenValid(String token, String username) {
        String subject = extractUsername(token);
        return subject.equals(username) && !isExpired(token);
    }

    public boolean isExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(parseAllClaims(token));
    }

    private Claims parseAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey signingKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
