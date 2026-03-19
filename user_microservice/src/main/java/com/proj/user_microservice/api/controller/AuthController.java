package com.proj.user_microservice.api.controller;

import com.proj.user_microservice.api.dto.*;
import com.proj.user_microservice.application.usecase.LoginUseCase;
import com.proj.user_microservice.application.usecase.LogoutUseCase;
import com.proj.user_microservice.application.usecase.RefreshTokenUseCase;
import com.proj.user_microservice.application.usecase.RegisterUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RegisterUseCase registerUseCase;
    private final LoginUseCase loginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse data = registerUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "User registered successfully", data));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse data = loginUseCase.execute(request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Login successful", data));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        RefreshTokenResponse data = refreshTokenUseCase.execute(request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Token refreshed", data));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader("X-User-Id") String userId) {
        logoutUseCase.execute(userId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Logged out successfully", null));
    }
}
