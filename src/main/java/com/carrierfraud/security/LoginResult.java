package com.carrierfraud.security;

import com.carrierfraud.security.dto.AuthResponse;

public record LoginResult(String token, AuthResponse body) {
}
