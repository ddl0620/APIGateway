package com.proj.payment_microservice.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        String id,
        String userId,
        BigDecimal amount,
        String currency,
        String status,
        String description,
        String gatewayTransactionId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
