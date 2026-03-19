package com.proj.user_microservice.api.controller;

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
    public RouterFunction<ServerResponse> legacyAuthRoutes() {
        return route(path("/api/auth/**"), request -> {
            String newPath = request.uri().getPath().replace("/api/auth/", "/api/v1/auth/");
            String query = request.uri().getQuery();
            String redirect = query != null ? newPath + "?" + query : newPath;
            return ServerResponse.temporaryRedirect(URI.create(redirect)).build();
        });
    }

    @Bean
    public RouterFunction<ServerResponse> legacyUserRoutes() {
        return route(path("/api/users/**"), request -> {
            String newPath = request.uri().getPath().replace("/api/users/", "/api/v1/users/");
            String query = request.uri().getQuery();
            String redirect = query != null ? newPath + "?" + query : newPath;
            return ServerResponse.temporaryRedirect(URI.create(redirect)).build();
        });
    }
}
