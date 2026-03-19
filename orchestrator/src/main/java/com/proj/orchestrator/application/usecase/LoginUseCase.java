package com.proj.orchestrator.application.usecase;

import com.proj.orchestrator.api.dto.ApiResponse;
import com.proj.orchestrator.api.dto.LoginRequest;
import com.proj.orchestrator.infrastructure.client.UserServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class LoginUseCase {

    private final UserServiceClient userServiceClient;

    public ApiResponse<Map<String, Object>> execute(LoginRequest request) {
        return userServiceClient.login(request);
    }
}
