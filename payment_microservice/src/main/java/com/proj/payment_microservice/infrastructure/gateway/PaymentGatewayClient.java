package com.proj.payment_microservice.infrastructure.gateway;

import com.proj.payment_microservice.exception.PaymentProcessingException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Component
public class PaymentGatewayClient {

    private final RestClient restClient;
    private final String chargePath;
    private final String refundPath;

    public PaymentGatewayClient(
            ObservationRegistry observationRegistry,
            @Value("${payment.gateway.base-url}") String baseUrl,
            @Value("${payment.gateway.charge-path}") String chargePath,
            @Value("${payment.gateway.refund-path}") String refundPath) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(new org.springframework.http.client.SimpleClientHttpRequestFactory()) // Forces HTTP/1.1
                .observationRegistry(observationRegistry)
                .build();
        this.chargePath = chargePath;
        this.refundPath = refundPath;
    }

    @CircuitBreaker(name = "paymentGateway", fallbackMethod = "chargeFallback")
    @Retry(name = "paymentGateway")
    public GatewayResponse charge(BigDecimal amount, String currency, String description) {
        log.info("Calling payment gateway: charge {} {}", amount, currency);
        try {
            Map<String, Object> body = Map.of(
                    "amount", amount.toString(),
                    "currency", currency,
                    "description", description
            );

            return restClient.post()
                    .uri(chargePath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(GatewayResponse.class);
        } catch (HttpClientErrorException e) {
            log.warn("Payment gateway returned error: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode().value() == 402) {
                return new GatewayResponse(null, "DECLINED", "Payment declined by gateway");
            }
            throw new PaymentProcessingException("Payment gateway error: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Payment gateway call failed", e);
            throw new PaymentProcessingException("Failed to connect to payment gateway", e);
        }
    }

    public GatewayResponse chargeFallback(BigDecimal amount, String currency, String description, Throwable t) {
        log.error("Circuit breaker open for payment gateway, returning FAILED for charge {} {}", amount, currency, t);
        return new GatewayResponse(null, "FAILED", "Payment gateway unavailable, please try again later");
    }

    @CircuitBreaker(name = "paymentGateway", fallbackMethod = "refundFallback")
    public GatewayResponse refund(String transactionId) {
        log.info("Calling payment gateway: refund {}", transactionId);
        try {
            Map<String, String> body = Map.of("transactionId", transactionId);

            return restClient.post()
                    .uri(refundPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(GatewayResponse.class);
        } catch (Exception e) {
            log.error("Refund gateway call failed", e);
            throw new PaymentProcessingException("Failed to process refund", e);
        }
    }

    public GatewayResponse refundFallback(String transactionId, Throwable t) {
        log.error("Circuit breaker open for refund {}", transactionId, t);
        throw new PaymentProcessingException("Payment gateway unavailable for refund");
    }

    public record GatewayResponse(String transactionId, String status, String message) {}
}
