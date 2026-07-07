package com.carrierfraud.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int LOGIN_REQUESTS_PER_MINUTE = 5;
    private static final int GENERAL_REQUESTS_PER_MINUTE = 60;

    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> generalBuckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String clientKey = resolveClientKey(request);
        String path = request.getRequestURI();

        Bucket bucket = "/api/v1/auth/login".equals(path)
                ? loginBuckets.computeIfAbsent(clientKey, k -> newBucket(LOGIN_REQUESTS_PER_MINUTE))
                : generalBuckets.computeIfAbsent(clientKey, k -> newBucket(GENERAL_REQUESTS_PER_MINUTE));

        if (!bucket.tryConsume(1)) {
            rejectWithTooManyRequests(response, path);
            return;
        }

        chain.doFilter(request, response);
    }

    private String resolveClientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded != null && !forwarded.isBlank()
                ? forwarded.split(",")[0].trim()
                : request.getRemoteAddr();
    }

    private Bucket newBucket(int requestsPerMinute) {
        return Bucket.builder()
                .addLimit(Bandwidth.simple(requestsPerMinute, Duration.ofMinutes(1)))
                .build();
    }

    private void rejectWithTooManyRequests(HttpServletResponse response, String path) throws IOException {
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write(String.format(
                "{\"type\":\"https://fraudsentinel.io/errors/too-many-requests\"," +
                        "\"title\":\"Too Many Requests\",\"status\":429," +
                        "\"detail\":\"Rate limit exceeded. Try again in a minute.\"," +
                        "\"instance\":\"%s\"}", path));
    }
}