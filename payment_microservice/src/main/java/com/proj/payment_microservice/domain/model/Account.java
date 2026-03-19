package com.proj.payment_microservice.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "accounts")
public class Account {

    @Id
    private String id;

    @Indexed(unique = true)
    private String userId;

    @Builder.Default
    private BigDecimal balance = new BigDecimal("50000.00");

    @Builder.Default
    private String currency = "USD";

    @Version
    private Long version;

    @CreatedDate
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
