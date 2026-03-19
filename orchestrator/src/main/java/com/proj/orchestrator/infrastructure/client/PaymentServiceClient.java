package com.proj.orchestrator.infrastructure.client;

import com.proj.orchestrator.api.dto.ApiResponse;
import com.proj.orchestrator.api.dto.PaymentResponse;
import com.proj.orchestrator.api.dto.ProcessPaymentRequest;
import com.proj.orchestrator.exception.ServiceException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

@Component
public class PaymentServiceClient {

    private final RestClient restClient;

    public PaymentServiceClient(@Qualifier("paymentServiceRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public ApiResponse<PaymentResponse> processPayment(String userId, ProcessPaymentRequest request) {
        try {
            return restClient.post()
                    .uri("/api/v1/payments/process")
                    .header("X-User-Id", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (RestClientResponseException ex) {
            throw new ServiceException("payment-service", ex.getStatusCode().value(), ex.getResponseBodyAsString());
        }
    }

    public ApiResponse<List<PaymentResponse>> getPaymentHistory(String userId) {
        try {
            return restClient.get()
                    .uri("/api/v1/payments/history")
                    .header("X-User-Id", userId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (RestClientResponseException ex) {
            throw new ServiceException("payment-service", ex.getStatusCode().value(), ex.getResponseBodyAsString());
        }
    }
}
