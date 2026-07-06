package com.carrierfraud.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NoteRequest(
        @NotBlank(message = "Note content is required")
        @Size(min = 1, max = 5000, message = "Note must be 1-5000 characters")
        String content
) {}
