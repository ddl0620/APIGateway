package com.proj.user_microservice.application.usecase;

import com.proj.user_microservice.api.dto.RefreshTokenRequest;
import com.proj.user_microservice.api.dto.RefreshTokenResponse;
import com.proj.user_microservice.application.service.AuditService;
import com.proj.user_microservice.domain.model.User;
import com.proj.user_microservice.domain.repository.UserRepository;
import com.proj.user_microservice.exception.InvalidCredentialsException;
import com.proj.user_microservice.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenUseCase {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuditService auditService;

    public RefreshTokenResponse execute(RefreshTokenRequest request) {
        User user = userRepository.findByRefreshToken(request.getRefreshToken())
                .filter(u -> !u.isDeleted())
                .filter(u -> u.getRefreshTokenExpiresAt() != null
                        && u.getRefreshTokenExpiresAt().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> {
                    log.warn("Refresh token invalid or expired");
                    return new InvalidCredentialsException("Invalid or expired refresh token");
                });

        // Rotate: generate new access + refresh tokens
        String newAccessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole().name());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken();

        user.setRefreshToken(newRefreshToken);
        user.setRefreshTokenExpiresAt(LocalDateTime.now().plusSeconds(
                jwtTokenProvider.getRefreshTokenExpirationMs() / 1000));
        userRepository.save(user);

        log.info("Token refreshed for userId={}", user.getId());
        auditService.log(user.getId(), "TOKEN_REFRESH", "USER", user.getId(),
                "Token refreshed", true);

        return new RefreshTokenResponse(newAccessToken, newRefreshToken);
    }
}
