package com.carrierfraud.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginRequest(

        @NotBlank
        @Size(min = 3, max = 50)
        @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "Username contains invalid characters")
        String username,

        @NotBlank
        @Size(min = 12, max = 100, message = "Password must be at least 12 characters")
        String password
) {
}