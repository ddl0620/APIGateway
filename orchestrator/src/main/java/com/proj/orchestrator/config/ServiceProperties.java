package com.proj.orchestrator.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "services")
public class ServiceProperties {

    private ServiceUrl user;
    private ServiceUrl payment;
    private String internalApiKey;

    @Getter
    @Setter
    public static class ServiceUrl {
        private String baseUrl;
    }
}
