package com.proj.orchestrator.application.usecase;

import com.proj.orchestrator.api.dto.ApiResponse;
import com.proj.orchestrator.api.dto.PaymentResponse;
import com.proj.orchestrator.api.dto.ProcessPaymentRequest;
import com.proj.orchestrator.infrastructure.client.PaymentServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProcessPaymentUseCase {

    private final PaymentServiceClient paymentServiceClient;

    public ApiResponse<PaymentResponse> execute(String userId, ProcessPaymentRequest request) {
        return paymentServiceClient.processPayment(userId, request);
    }
}
