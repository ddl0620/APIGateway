package com.proj.user_microservice.api.controller;

import com.proj.user_microservice.api.dto.ApiResponse;
import com.proj.user_microservice.api.dto.UserProfileResponse;
import com.proj.user_microservice.application.usecase.GetProfileUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final GetProfileUseCase getProfileUseCase;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(
            @RequestHeader("X-User-Id") String userId) {
        UserProfileResponse data = getProfileUseCase.execute(userId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "User profile retrieved", data));
    }
}
