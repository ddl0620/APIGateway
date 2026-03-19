package com.proj.orchestrator.api.controller;

import com.proj.orchestrator.api.dto.ApiResponse;
import com.proj.orchestrator.api.dto.PaymentResponse;
import com.proj.orchestrator.api.dto.ProcessPaymentRequest;
import com.proj.orchestrator.application.usecase.GetPaymentHistoryUseCase;
import com.proj.orchestrator.application.usecase.ProcessPaymentUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final ProcessPaymentUseCase processPaymentUseCase;
    private final GetPaymentHistoryUseCase getPaymentHistoryUseCase;

    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    @PostMapping("/process")
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(
            Authentication authentication,
            @Valid @RequestBody ProcessPaymentRequest request) {
        String userId = (String) authentication.getPrincipal();
        ApiResponse<PaymentResponse> response = processPaymentUseCase.execute(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getPaymentHistory(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        ApiResponse<List<PaymentResponse>> response = getPaymentHistoryUseCase.execute(userId);
        return ResponseEntity.ok(response);
    }
}
