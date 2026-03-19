package com.proj.payment_microservice.application.usecase;

import com.proj.payment_microservice.api.dto.PaymentResponse;
import com.proj.payment_microservice.application.service.AuditService;
import com.proj.payment_microservice.domain.model.Payment;
import com.proj.payment_microservice.domain.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetPaymentHistoryUseCase {

    private final PaymentRepository paymentRepository;
    private final AuditService auditService;

    public List<PaymentResponse> execute(String userId) {
        log.info("Fetching payment history for user {}", userId);
        List<Payment> payments = paymentRepository.findByUserIdOrderByCreatedAtDesc(userId);

        auditService.log(userId, "PAYMENT_HISTORY_VIEW", "PAYMENT", null,
                "Viewed payment history (" + payments.size() + " records)", true);

        return payments.stream()
                .map(this::toResponse)
                .toList();
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
