package com.proj.orchestrator.exception;

import lombok.Getter;

@Getter
public class ServiceException extends RuntimeException {

    private final String serviceName;
    private final int statusCode;
    private final String responseBody;

    public ServiceException(String serviceName, int statusCode, String responseBody) {
        super("Service [%s] returned status %d".formatted(serviceName, statusCode));
        this.serviceName = serviceName;
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }
}
