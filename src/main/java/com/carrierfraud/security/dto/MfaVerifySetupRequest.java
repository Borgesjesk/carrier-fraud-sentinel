package com.carrierfraud.security.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record MfaVerifySetupRequest(
        @Min(value = 0, message = "code must be positive")
        @Max(value = 999999, message = "code must be 6 digits")
        int code
) {}
