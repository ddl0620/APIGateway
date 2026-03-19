package com.proj.orchestrator.application.usecase;

import com.proj.orchestrator.api.dto.ApiResponse;
import com.proj.orchestrator.api.dto.PaymentResponse;
import com.proj.orchestrator.infrastructure.client.PaymentServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetPaymentHistoryUseCase {

    private final PaymentServiceClient paymentServiceClient;

    public ApiResponse<List<PaymentResponse>> execute(String userId) {
        return paymentServiceClient.getPaymentHistory(userId);
    }
}
