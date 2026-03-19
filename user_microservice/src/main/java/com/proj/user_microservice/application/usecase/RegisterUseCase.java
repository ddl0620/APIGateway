package com.proj.user_microservice.application.usecase;

import com.proj.user_microservice.api.dto.RegisterRequest;
import com.proj.user_microservice.api.dto.RegisterResponse;
import com.proj.user_microservice.application.service.AuditService;
import com.proj.user_microservice.domain.model.User;
import com.proj.user_microservice.domain.repository.UserRepository;
import com.proj.user_microservice.exception.DuplicateResourceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegisterUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Transactional
    public RegisterResponse execute(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed: email already in use: {}", request.getEmail());
            auditService.log(null, "REGISTER_FAILED", "USER", null,
                    "Duplicate email: " + request.getEmail(), false);
            throw new DuplicateResourceException("Email already in use");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .build();

        User saved = userRepository.save(user);
        log.info("User registered: id={}, email={}", saved.getId(), saved.getEmail());
        auditService.log(saved.getId(), "REGISTER_SUCCESS", "USER", saved.getId(),
                "User registered: " + saved.getEmail(), true);

        return new RegisterResponse(
                saved.getId(),
                saved.getEmail(),
                saved.getFirstName(),
                saved.getLastName()
        );
    }
}
