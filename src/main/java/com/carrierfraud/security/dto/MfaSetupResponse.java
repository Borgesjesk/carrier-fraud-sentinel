package com.carrierfraud.security.dto;

public record MfaSetupResponse(String secret, String otpauthUri) {}
