package com.carrierfraud.api;

public record TransactionRequest {
    @NotBlank(message = "Carrier name is required")
}
