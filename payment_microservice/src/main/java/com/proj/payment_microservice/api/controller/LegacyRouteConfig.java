package com.proj.payment_microservice.api.controller;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import java.net.URI;

import static org.springframework.web.servlet.function.RequestPredicates.path;
import static org.springframework.web.servlet.function.RouterFunctions.route;

@Configuration
public class LegacyRouteConfig {

    @Bean
    public RouterFunction<ServerResponse> legacyPaymentRoutes() {
        return route(path("/api/payments/**"), request -> {
            String newPath = request.uri().getPath().replace("/api/payments/", "/api/v1/payments/");
            String query = request.uri().getQuery();
            String redirect = query != null ? newPath + "?" + query : newPath;
            return ServerResponse.temporaryRedirect(URI.create(redirect)).build();
        });
    }
}
