package com.carrierfraud.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ComplaintRequest(
        @NotBlank(message = "Carrier name is required")
        @Size(min = 2, max = 100, message = "Carrier name must be 2-100 characters")
        String carrierName,

        @NotBlank(message = "Description is required")
        @Size(min = 20, max = 2000, message = "Description must be 20-2000 characters")
        String description,

        @NotBlank(message = "Complaint type is required")
        String complaintType
) {}
