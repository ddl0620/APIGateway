package com.proj.orchestrator.api.controller;

import com.proj.orchestrator.api.dto.ApiResponse;
import com.proj.orchestrator.api.dto.UserProfileResponse;
import com.proj.orchestrator.application.usecase.GetProfileUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final GetProfileUseCase getProfileUseCase;

    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        ApiResponse<UserProfileResponse> response = getProfileUseCase.execute(userId);
        return ResponseEntity.ok(response);
    }
}
