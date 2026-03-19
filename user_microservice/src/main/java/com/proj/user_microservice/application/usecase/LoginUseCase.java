package com.proj.user_microservice.application.usecase;

import com.proj.user_microservice.api.dto.LoginRequest;
import com.proj.user_microservice.api.dto.LoginResponse;
import com.proj.user_microservice.application.service.AuditService;
import com.proj.user_microservice.security.JwtTokenProvider;
import com.proj.user_microservice.domain.model.User;
import com.proj.user_microservice.domain.repository.UserRepository;
import com.proj.user_microservice.exception.InvalidCredentialsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginUseCase {

        private static final int MAX_FAILED_ATTEMPTS = 5;
        private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuditService auditService;

    public LoginResponse execute(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> {
                    log.warn("Login failed: no active account for email={}", maskEmail(request.getEmail()));
                    auditService.log(null, "LOGIN_FAILED", "USER", null,
                            "No active account", false);
                    return new InvalidCredentialsException("Invalid email or password");
                });

        if (user.getAccountLockedUntil() != null && user.getAccountLockedUntil().isAfter(LocalDateTime.now())) {
            log.warn("Login rejected: account locked for userId={}", user.getId());
            auditService.log(user.getId(), "LOGIN_BLOCKED", "USER", user.getId(),
                    "Account temporarily locked", false);
            throw new InvalidCredentialsException("Account temporarily locked. Try again later.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login failed: wrong password for email={}", maskEmail(request.getEmail()));

            int failedAttempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(failedAttempts);
            if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
                user.setAccountLockedUntil(LocalDateTime.now().plus(LOCK_DURATION));
                user.setFailedLoginAttempts(0);
            }
            userRepository.save(user);

            auditService.log(user.getId(), "LOGIN_FAILED", "USER", user.getId(),
                    "Invalid credentials", false);
            throw new InvalidCredentialsException("Invalid email or password");
        }

        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);

        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken();

        // Store refresh token
        user.setRefreshToken(refreshToken);
        user.setRefreshTokenExpiresAt(LocalDateTime.now().plusSeconds(
                jwtTokenProvider.getRefreshTokenExpirationMs() / 1000));
        userRepository.save(user);

        log.info("User logged in: id={}, email={}", user.getId(), maskEmail(user.getEmail()));
        auditService.log(user.getId(), "LOGIN_SUCCESS", "USER", user.getId(),
                "Login successful", true);

        return new LoginResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().name(),
                accessToken,
                refreshToken
        );
    }

        private String maskEmail(String email) {
                if (email == null) {
                        return "***";
                }
                int at = email.indexOf('@');
                if (at <= 1) {
                        return "***";
                }
                return email.charAt(0) + "***" + email.substring(at);
        }
}
