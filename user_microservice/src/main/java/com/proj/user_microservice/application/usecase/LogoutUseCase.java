package com.proj.user_microservice.application.usecase;

import com.proj.user_microservice.application.service.AuditService;
import com.proj.user_microservice.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogoutUseCase {

    private final UserRepository userRepository;
    private final AuditService auditService;

    public void execute(String userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setRefreshToken(null);
            user.setRefreshTokenExpiresAt(null);
            userRepository.save(user);
            log.info("User logged out: id={}", userId);
            auditService.log(userId, "LOGOUT", "USER", userId, "User logged out", true);
        });
    }
}
