package com.proj.payment_microservice.api.controller;

import com.proj.payment_microservice.api.dto.ApiResponse;
import com.proj.payment_microservice.api.dto.PaymentResponse;
import com.proj.payment_microservice.api.dto.ProcessPaymentRequest;
import com.proj.payment_microservice.application.usecase.GetPaymentHistoryUseCase;
import com.proj.payment_microservice.application.usecase.ProcessPaymentUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final ProcessPaymentUseCase processPaymentUseCase;
    private final GetPaymentHistoryUseCase getPaymentHistoryUseCase;

    @PostMapping("/process")
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(
            @Valid @RequestBody ProcessPaymentRequest request,
            @RequestHeader("X-User-Id") String userId) {

        PaymentResponse response = processPaymentUseCase.execute(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(201, "Payment processed", response));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getPaymentHistory(
            @RequestHeader("X-User-Id") String userId) {

        List<PaymentResponse> history = getPaymentHistoryUseCase.execute(userId);

        return ResponseEntity.ok(new ApiResponse<>(200, "Payment history retrieved", history));
    }
}
