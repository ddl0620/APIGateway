package com.proj.orchestrator.api.dto;

import java.time.LocalDateTime;

public record UserProfileResponse(
        String id,
        String email,
        String firstName,
        String lastName,
        String role,
        LocalDateTime createdAt
) {
}
