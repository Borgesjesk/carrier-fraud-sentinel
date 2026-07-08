package com.carrierfraud.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "token is required")
        String token,

        @NotBlank(message = "newPassword is required")
        @Size(min = 12, max = 128, message = "newPassword must be 12-128 characters")
        String newPassword
) {}
