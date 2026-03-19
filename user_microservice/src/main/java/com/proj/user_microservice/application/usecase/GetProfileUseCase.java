package com.proj.user_microservice.application.usecase;

import com.proj.user_microservice.api.dto.UserProfileResponse;
import com.proj.user_microservice.application.service.AuditService;
import com.proj.user_microservice.domain.model.User;
import com.proj.user_microservice.domain.repository.UserRepository;
import com.proj.user_microservice.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetProfileUseCase {

    private final UserRepository userRepository;
    private final AuditService auditService;

    public UserProfileResponse execute(String userId) {
        User user = userRepository.findById(userId)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> {
                    log.warn("Profile lookup failed: user not found for id={}", userId);
                    return new ResourceNotFoundException("User not found");
                });

        log.debug("Profile retrieved for userId={}", userId);
        auditService.log(userId, "PROFILE_VIEW", "USER", userId,
                "Profile viewed", true);

        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().name(),
                user.getCreatedAt()
        );
    }
}
