package com.proj.orchestrator.application.usecase;

import com.proj.orchestrator.api.dto.ApiResponse;
import com.proj.orchestrator.api.dto.RegisterRequest;
import com.proj.orchestrator.infrastructure.client.UserServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class RegisterUseCase {

    private final UserServiceClient userServiceClient;

    public ApiResponse<Map<String, Object>> execute(RegisterRequest request) {
        return userServiceClient.register(request);
    }
}
