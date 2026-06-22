package com.carrierfraud.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.cookie")
public record CookieProperties(
        @NotBlank String name,
        boolean secure,
        @NotBlank @Pattern(regexp = "Strict|Lax|None") String sameSite,
        @Positive long maxAgeSeconds,
        @NotBlank String path
) {
}
