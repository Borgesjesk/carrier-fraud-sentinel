package com.carrierfraud.security.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record LoginMfaRequest(
        @NotBlank String username,
        @NotBlank String password,
        @Min(0) @Max(999999) int code
) {}
