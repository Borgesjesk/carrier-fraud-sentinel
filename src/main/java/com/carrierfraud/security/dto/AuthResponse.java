package com.carrierfraud.security.dto;

public record AuthResponse(String accessToken, String tokenType, String role) {
}
