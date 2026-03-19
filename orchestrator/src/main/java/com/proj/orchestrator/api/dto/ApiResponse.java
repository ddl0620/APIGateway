package com.proj.orchestrator.api.dto;

public record ApiResponse<T>(
        int code,
        String message,
        T data
) {
}
