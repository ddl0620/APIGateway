package com.proj.payment_microservice.application.usecase;

import com.proj.payment_microservice.api.dto.PaymentResponse;
import com.proj.payment_microservice.api.dto.ProcessPaymentRequest;
import com.proj.payment_microservice.application.service.AuditService;
import com.proj.payment_microservice.domain.model.*;
import com.proj.payment_microservice.domain.repository.AccountRepository;
import com.proj.payment_microservice.domain.repository.LedgerEntryRepository;
import com.proj.payment_microservice.domain.repository.PaymentRepository;
import com.proj.payment_microservice.exception.InsufficientBalanceException;
import com.proj.payment_microservice.exception.PaymentProcessingException;
import com.proj.payment_microservice.exception.ResourceNotFoundException;
import com.proj.payment_microservice.infrastructure.gateway.PaymentGatewayClient;
import com.proj.payment_microservice.infrastructure.gateway.PaymentGatewayClient.GatewayResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessPaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final PaymentGatewayClient paymentGatewayClient;
    private final AuditService auditService;

    private static final int MAX_DEBIT_RETRIES = 3;

    public PaymentResponse execute(String userId, ProcessPaymentRequest request) {
        log.info("Processing payment for user {} amount {} {} idempotencyKey={}",
                userId, request.getAmount(), request.getCurrency(), maskIdempotencyKey(request.getIdempotencyKey()));

        // 1. Idempotency check scoped per user
        Optional<Payment> existing = paymentRepository.findByUserIdAndIdempotencyKey(userId, request.getIdempotencyKey());
        if (existing.isPresent()) {
            log.info("Idempotent request detected, returning existing payment id={}", existing.get().getId());
            return toResponse(existing.get());
        }

        // 2. Get or create account for user
        Account account = accountRepository.findByUserId(userId)
                .orElseGet(() -> accountRepository.save(Account.builder()
                        .userId(userId)
                        .build()));

        // 3. Balance check
        if (request.getAmount().compareTo(account.getBalance()) > 0) {
            auditService.log(userId, "PAYMENT_REJECTED", "PAYMENT", null,
                    "Insufficient balance. Requested: " + request.getAmount()
                            + ", Available: " + account.getBalance(), false);
            throw new InsufficientBalanceException(
                    "Insufficient balance. Available: " + account.getBalance());
        }

        // 4. Create payment record with PENDING status
        Currency currency = Currency.valueOf(request.getCurrency().toUpperCase());
        Payment payment = Payment.builder()
                .userId(userId)
                .idempotencyKey(request.getIdempotencyKey())
                .amount(request.getAmount())
                .currency(currency)
                .status(PaymentStatus.PENDING)
                .description(request.getDescription())
                .build();
        try {
            payment = paymentRepository.save(payment);
        } catch (DuplicateKeyException ex) {
            Payment already = paymentRepository
                    .findByUserIdAndIdempotencyKey(userId, request.getIdempotencyKey())
                    .orElseThrow(() -> ex);
            return toResponse(already);
        }
        log.info("Payment created with id {} in PENDING status", payment.getId());

        boolean debited = false;

        // 5. Call payment gateway with circuit breaker
        try {
            debitWithRetry(userId, request.getAmount(), payment.getId());
            debited = true;

            GatewayResponse gatewayResponse = paymentGatewayClient.charge(
                    request.getAmount(),
                    currency.name(),
                    request.getDescription() != null ? request.getDescription() : ""
            );

            if ("CAPTURED".equals(gatewayResponse.status())) {
                payment.setStatus(PaymentStatus.SUCCESS);
                payment.setGatewayTransactionId(gatewayResponse.transactionId());
                payment.setGatewayResponse(gatewayResponse.message());

                auditService.log(userId, "PAYMENT_SUCCESS", "PAYMENT", payment.getId(),
                        "Payment successful: " + request.getAmount() + " " + currency, true);
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setGatewayResponse(gatewayResponse.message());
                if (debited) {
                    compensateCredit(userId, request.getAmount(), payment.getId());
                }
                auditService.log(userId, "PAYMENT_FAILED", "PAYMENT", payment.getId(),
                        "Gateway declined: " + gatewayResponse.message(), false);
            }
        } catch (Exception e) {
            log.error("Gateway call failed for payment {}", payment.getId(), e);
            payment.setStatus(PaymentStatus.FAILED);
            payment.setGatewayResponse("Gateway error: " + e.getMessage());
            if (debited) {
                compensateCredit(userId, request.getAmount(), payment.getId());
            }
            auditService.log(userId, "PAYMENT_FAILED", "PAYMENT", payment.getId(),
                    "Gateway error: " + e.getMessage(), false);
        }

        payment.setUpdatedAt(LocalDateTime.now());
        payment = paymentRepository.save(payment);
        log.info("Payment {} updated to status {}", payment.getId(), payment.getStatus());

        return toResponse(payment);
    }

    private void debitWithRetry(String userId, BigDecimal amount, String paymentId) {
        for (int attempt = 1; attempt <= MAX_DEBIT_RETRIES; attempt++) {
            Account account = accountRepository.findByUserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Account not found for user " + userId));

            BigDecimal balanceBefore = account.getBalance();
            if (amount.compareTo(balanceBefore) > 0) {
                throw new InsufficientBalanceException("Insufficient balance");
            }

            BigDecimal balanceAfter = balanceBefore.subtract(amount);
            account.setBalance(balanceAfter);
            account.setUpdatedAt(LocalDateTime.now());

            try {
                Account saved = accountRepository.save(account);

                LedgerEntry entry = LedgerEntry.builder()
                        .accountId(saved.getId())
                        .paymentId(paymentId)
                        .type("DEBIT")
                        .amount(amount)
                        .balanceBefore(balanceBefore)
                        .balanceAfter(balanceAfter)
                        .description("Payment debit")
                        .build();
                ledgerEntryRepository.save(entry);
                return;
            } catch (OptimisticLockingFailureException e) {
                log.warn("Optimistic lock failure for user {}, attempt {}", userId, attempt);
                if (attempt == MAX_DEBIT_RETRIES) {
                    throw new PaymentProcessingException("Concurrent account modification, please retry", e);
                }
            }
        }
    }

    private void compensateCredit(String userId, BigDecimal amount, String paymentId) {
        Account account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found for user " + userId));

        BigDecimal balanceBefore = account.getBalance();
        BigDecimal balanceAfter = balanceBefore.add(amount);

        account.setBalance(balanceAfter);
        account.setUpdatedAt(LocalDateTime.now());
        Account saved = accountRepository.save(account);

        LedgerEntry entry = LedgerEntry.builder()
                .accountId(saved.getId())
                .paymentId(paymentId)
                .type("CREDIT")
                .amount(amount)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .description("Compensation credit for failed payment")
                .build();
        ledgerEntryRepository.save(entry);
    }

    private String maskIdempotencyKey(String key) {
        if (key == null || key.length() < 8) {
            return "***";
        }
        return key.substring(0, 4) + "****" + key.substring(key.length() - 4);
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getCurrency().name(),
                payment.getStatus().name(),
                payment.getDescription(),
                payment.getGatewayTransactionId(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}
