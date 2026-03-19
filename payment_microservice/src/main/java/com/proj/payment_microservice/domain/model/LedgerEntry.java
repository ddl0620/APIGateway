package com.proj.payment_microservice.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "ledger_entries")
@CompoundIndex(name = "idx_account_created", def = "{'accountId': 1, 'createdAt': -1}")
public class LedgerEntry {

    @Id
    private String id;

    private String accountId;

    private String paymentId;

    private String type;  // DEBIT or CREDIT

    private BigDecimal amount;

    private BigDecimal balanceBefore;

    private BigDecimal balanceAfter;

    private String description;

    @CreatedDate
    private LocalDateTime createdAt;
}
