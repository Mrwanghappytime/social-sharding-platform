package com.social.gateway.config;

import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder.Builder;

import java.util.Arrays;
import java.util.List;

/**
 * Route definitions for the Gateway service.
 *
 * Routes configured in application.yml:
 * - /api/** -> facade-service:8087
 * - /files/** -> facade-service:8087 (proxies to file-service)
 */
@Configuration
public class RouteDefinitions {

    // Route IDs as constants for reference
    public static final String USER_SERVICE = "user-service";
    public static final String POST_SERVICE = "post-service";
    public static final String INTERACTION_SERVICE = "interaction-service";
    public static final String RELATION_SERVICE = "relation-service";
    public static final String NOTIFICATION_SERVICE = "notification-service";
    public static final String FILE_SERVICE = "file-service";
}
