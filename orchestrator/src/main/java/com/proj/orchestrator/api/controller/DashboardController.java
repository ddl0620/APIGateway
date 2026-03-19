package com.proj.orchestrator.api.controller;

import com.proj.orchestrator.api.dto.ApiResponse;
import com.proj.orchestrator.api.dto.DashboardResponse;
import com.proj.orchestrator.application.usecase.GetDashboardUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final GetDashboardUseCase getDashboardUseCase;

    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        ApiResponse<DashboardResponse> response = getDashboardUseCase.execute(userId);
        return ResponseEntity.ok(response);
    }
}
