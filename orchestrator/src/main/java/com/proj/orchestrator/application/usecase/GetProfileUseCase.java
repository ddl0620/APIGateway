package com.proj.orchestrator.application.usecase;

import com.proj.orchestrator.api.dto.ApiResponse;
import com.proj.orchestrator.api.dto.UserProfileResponse;
import com.proj.orchestrator.infrastructure.client.UserServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetProfileUseCase {

    private final UserServiceClient userServiceClient;

    public ApiResponse<UserProfileResponse> execute(String userId) {
        return userServiceClient.getProfile(userId);
    }
}
