package com.proj.orchestrator.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProcessPaymentRequest(
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotBlank String currency,
        String description,
        @NotBlank String idempotencyKey
) {
}
