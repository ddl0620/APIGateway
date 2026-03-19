package com.proj.user_microservice.domain.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String password;

    private String firstName;

    private String lastName;

    @Builder.Default
    private Role role = Role.CUSTOMER;

    private String refreshToken;

    private LocalDateTime refreshTokenExpiresAt;

    @Builder.Default
    private int failedLoginAttempts = 0;

    private LocalDateTime accountLockedUntil;

    @Builder.Default
    private boolean deleted = false;

    private LocalDateTime deletedAt;

    @CreatedDate
    private LocalDateTime createdAt;
}
