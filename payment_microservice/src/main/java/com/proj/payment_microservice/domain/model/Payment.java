package com.proj.payment_microservice.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "payments")
@CompoundIndex(name = "uk_user_idempotency", def = "{'userId': 1, 'idempotencyKey': 1}", unique = true)
public class Payment {

    @Id
    private String id;

    @Indexed
    private String userId;

    @Indexed(sparse = true)
    private String idempotencyKey;

    private BigDecimal amount;

    private Currency currency;

    private PaymentStatus status;

    private String description;

    private String gatewayTransactionId;

    private String gatewayResponse;

    @CreatedDate
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
