package com.carrierfraud.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ROLE_PREFIX = "ROLE_";
    private static final String USER_AGENT_HEADER = "User-Agent";
    private static final Set<String> VALID_ROLES = Set.of("ANALYST", "ADMIN", "COMPLIANCE");

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain
    ) throws ServletException, IOException {

        String header = request.getHeader(AUTH_HEADER);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }

        try {
            authenticateToken(token, request);
        } catch (JwtException ex) {
            log.warn("Invalid JWT: uri={} remote={} ua={} reason={}",
                    request.getRequestURI(),
                    request.getRemoteAddr(),
                    request.getHeader(USER_AGENT_HEADER),
                    ex.getClass().getSimpleName());
            SecurityContextHolder.clearContext();
        }

        chain.doFilter(request, response);
    }

    private void authenticateToken(String token, HttpServletRequest request) {
        String username = jwtService.extractUsername(token);
        if (username == null
                || SecurityContextHolder.getContext().getAuthentication() != null
                || !jwtService.isTokenValid(token, username)) {
            return;
        }

        String role = extractValidRole(token);
        List<SimpleGrantedAuthority> authorities = role == null
                ? List.of()
                : List.of(new SimpleGrantedAuthority(ROLE_PREFIX + role));

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(username, null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String extractValidRole(String token) {
        Object claim = jwtService.extractClaim(token, c -> c.get("role"));
        if (!(claim instanceof String role) || role.isBlank()) {
            return null;
        }
        if (!VALID_ROLES.contains(role)) {
            log.warn("Rejected unknown role claim: {}", role);
            return null;
        }
        return role;
    }
}