package com.carrierfraud.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentRequest(
        @NotBlank(message = "Comment content is required")
        @Size(min = 1, max = 2000, message = "Comment must be 1-2000 characters")
        String content
) {}
