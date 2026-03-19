package com.proj.orchestrator.exception;

public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException(String serviceName, Throwable cause) {
        super("Service [%s] is unavailable".formatted(serviceName), cause);
    }
}
