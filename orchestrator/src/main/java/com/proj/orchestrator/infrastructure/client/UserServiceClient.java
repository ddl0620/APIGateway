package com.proj.orchestrator.infrastructure.client;

import com.proj.orchestrator.api.dto.ApiResponse;
import com.proj.orchestrator.api.dto.LoginRequest;
import com.proj.orchestrator.api.dto.RegisterRequest;
import com.proj.orchestrator.api.dto.UserProfileResponse;
import com.proj.orchestrator.exception.ServiceException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@Component
public class UserServiceClient {

    private final RestClient restClient;

    public UserServiceClient(@Qualifier("userServiceRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public ApiResponse<Map<String, Object>> register(RegisterRequest request) {
        try {
            return restClient.post()
                    .uri("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (RestClientResponseException ex) {
            throw new ServiceException("user-service", ex.getStatusCode().value(), ex.getResponseBodyAsString());
        }
    }

    public ApiResponse<Map<String, Object>> login(LoginRequest request) {
        try {
            return restClient.post()
                    .uri("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (RestClientResponseException ex) {
            throw new ServiceException("user-service", ex.getStatusCode().value(), ex.getResponseBodyAsString());
        }
    }

    public ApiResponse<Map<String, Object>> refreshToken(Map<String, String> request) {
        try {
            return restClient.post()
                    .uri("/api/v1/auth/refresh-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (RestClientResponseException ex) {
            throw new ServiceException("user-service", ex.getStatusCode().value(), ex.getResponseBodyAsString());
        }
    }

    public ApiResponse<Map<String, Object>> logout(String userId) {
        try {
            return restClient.post()
                    .uri("/api/v1/auth/logout")
                    .header("X-User-Id", userId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (RestClientResponseException ex) {
            throw new ServiceException("user-service", ex.getStatusCode().value(), ex.getResponseBodyAsString());
        }
    }

    public ApiResponse<UserProfileResponse> getProfile(String userId) {
        try {
            return restClient.get()
                    .uri("/api/v1/users/profile")
                    .header("X-User-Id", userId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (RestClientResponseException ex) {
            throw new ServiceException("user-service", ex.getStatusCode().value(), ex.getResponseBodyAsString());
        }
    }
}
