package com.carrierfraud.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final JwtService jwtService;

    public AuthController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Validated @RequestBody LoginRequest request) {

        String role = "admin".equalsIgnoreCase(request.username()) ? "ADMIN" : "USER";
        String token = jwtService.generateToken(request.username(), Map.of("role", role));
        return ResponseEntity.ok(new AuthResponse(token, "Bearer", role));
    }

    public record LoginRequest(
            @NotBlank
            @Size(min = 3, max = 50)
            @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "Username contains invalid characters")
            String username,

            @NotBlank
            @Size(min = 8, max = 100)
            String password
    ) {}

    public record AuthResponse(String accessToken, String tokenType, String role) {}
}
