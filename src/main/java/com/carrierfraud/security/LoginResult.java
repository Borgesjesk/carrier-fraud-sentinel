package com.carrierfraud.security;

import com.carrierfraud.security.dto.AuthResponse;

public record LoginResult(String token, AuthResponse body, boolean mfaRequired) {

    public static LoginResult authenticated(String token, AuthResponse body) {
        return new LoginResult(token, body, false);
    }

    public static LoginResult mfaChallenge() {
        return new LoginResult(null, null, true);
    }
}
