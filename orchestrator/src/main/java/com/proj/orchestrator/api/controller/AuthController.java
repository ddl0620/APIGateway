package com.proj.orchestrator.api.controller;

import com.proj.orchestrator.api.dto.ApiResponse;
import com.proj.orchestrator.api.dto.LoginRequest;
import com.proj.orchestrator.api.dto.RegisterRequest;
import com.proj.orchestrator.application.usecase.LoginUseCase;
import com.proj.orchestrator.application.usecase.RefreshTokenUseCase;
import com.proj.orchestrator.application.usecase.LogoutUseCase;
import com.proj.orchestrator.application.usecase.RegisterUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RegisterUseCase registerUseCase;
    private final LoginUseCase loginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(@Valid @RequestBody RegisterRequest request) {
        ApiResponse<Map<String, Object>> response = registerUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@Valid @RequestBody LoginRequest request) {
        ApiResponse<Map<String, Object>> response = loginUseCase.execute(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<Map<String, Object>>> refreshToken(
            @RequestBody Map<String, String> request) {
        ApiResponse<Map<String, Object>> response = refreshTokenUseCase.execute(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Map<String, Object>>> logout(
            Authentication authentication,
            @RequestHeader("Authorization") String authorizationHeader) {
        String userId = (String) authentication.getPrincipal();
        String accessToken = authorizationHeader.startsWith("Bearer ")
                ? authorizationHeader.substring(7)
                : authorizationHeader;

        ApiResponse<Map<String, Object>> response = logoutUseCase.execute(userId, accessToken);
        return ResponseEntity.ok(response);
    }
}
