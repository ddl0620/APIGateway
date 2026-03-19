package com.proj.orchestrator.config;

import com.proj.orchestrator.security.CorrelationIdFilter;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
public class RestClientConfig {

    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    private final ServiceProperties serviceProperties;
    private final ObservationRegistry observationRegistry;

    @Bean
    public RestClient userServiceRestClient() {
        return RestClient.builder()
                .baseUrl(serviceProperties.getUser().getBaseUrl())
                .defaultHeader(INTERNAL_API_KEY_HEADER, serviceProperties.getInternalApiKey())
                .observationRegistry(observationRegistry)
                .requestInitializer(request -> {
                    String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY);
                    if (correlationId != null) {
                        request.getHeaders().set(CorrelationIdFilter.CORRELATION_ID_HEADER, correlationId);
                    }
                })
                .build();
    }

    @Bean
    public RestClient paymentServiceRestClient() {
        return RestClient.builder()
                .baseUrl(serviceProperties.getPayment().getBaseUrl())
                .defaultHeader(INTERNAL_API_KEY_HEADER, serviceProperties.getInternalApiKey())
                .observationRegistry(observationRegistry)
                .requestInitializer(request -> {
                    String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY);
                    if (correlationId != null) {
                        request.getHeaders().set(CorrelationIdFilter.CORRELATION_ID_HEADER, correlationId);
                    }
                })
                .build();
    }
}
