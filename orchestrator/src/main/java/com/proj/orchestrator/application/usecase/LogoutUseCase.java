package com.proj.orchestrator.application.usecase;

import com.proj.orchestrator.api.dto.ApiResponse;
import com.proj.orchestrator.infrastructure.client.UserServiceClient;
import com.proj.orchestrator.security.JwtTokenProvider;
import com.proj.orchestrator.security.TokenRevocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LogoutUseCase {

    private final UserServiceClient userServiceClient;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenRevocationService tokenRevocationService;

    public ApiResponse<Map<String, Object>> execute(String userId, String accessToken) {
        String jti = jwtTokenProvider.getJti(accessToken);
        Instant expiresAt = jwtTokenProvider.parseToken(accessToken).getExpiration().toInstant();
        tokenRevocationService.revoke(jti, expiresAt);

        return userServiceClient.logout(userId);
    }
}
