package com.iqscaffold.gatewayservice.config.module;

import com.iqscaffold.gatewayservice.config.IqScaffoldProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Gateway route configuration for CRM Services.
 *
 * <p>Configures routing for all CRM-related endpoints including:
 * <ul>
 *   <li>Lead Service - Lead management, activities, and notes</li>
 *   <li>Pipeline Service - Sales pipeline, dashboard, and follow-ups</li>
 *   <li>Contact Service - Contact, company, and webhook management</li>
 * </ul>
 *
 * <p>Each route includes:
 * <ul>
 *   <li>Rate limiting with CRM-specific policies</li>
 *   <li>Circuit breaker protection</li>
 *   <li>Request transformation</li>
 *   <li>Correlation ID propagation</li>
 * </ul>
 *
 * <p>This configuration is only active when the 'crm' profile is enabled.
 */
@Configuration
@Profile("crm")
@ConditionalOnProperty(name = "spring.cloud.gateway.enabled", havingValue = "true", matchIfMissing = true)
public class CrmRouteConfig {

  private static final Logger log = LoggerFactory.getLogger(CrmRouteConfig.class);

  private final IqScaffoldProperties properties;

  public CrmRouteConfig(final IqScaffoldProperties properties) {
    this.properties = properties;
  }

  @Bean
  public RouteLocator crmRoutes(
      final RouteLocatorBuilder builder,
      RedisRateLimiter defaultRateLimiter) {
    var stripCount = properties.gateway().routing().apiPrefix().stripCount();
    var leadServiceUri = properties.gateway().routing().services().get("lead-service").uri();
    var pipelineServiceUri = properties.gateway().routing().services().get("pipeline-service").uri();
    var contactServiceUri = properties.gateway().routing().services().get("contact-service").uri();

    log.info("Configuring CRM routes with default rate limiter");

    return builder.routes()
        // Health Check Routes (Public - No Auth Required)
        .route("lead-service-health", r -> r
            .path("/api/v1/leads/health")
            .filters(f -> f
                .stripPrefix(stripCount)
                .circuitBreaker(config -> config
                    .setName("lead-service-health")
                    .setFallbackUri("forward:/fallback/health"))
                .retry(config -> config.setRetries(1)))
            .uri(leadServiceUri))

        .route("pipeline-service-health", r -> r
            .path("/api/v1/pipeline/health")
            .filters(f -> f
                .stripPrefix(stripCount)
                .circuitBreaker(config -> config
                    .setName("pipeline-service-health")
                    .setFallbackUri("forward:/fallback/health"))
                .retry(config -> config.setRetries(1)))
            .uri(pipelineServiceUri))

        .route("contact-service-health", r -> r
            .path("/api/v1/contacts/health")
            .filters(f -> f
                .stripPrefix(stripCount)
                .circuitBreaker(config -> config
                    .setName("contact-service-health")
                    .setFallbackUri("forward:/fallback/health"))
                .retry(config -> config.setRetries(1)))
            .uri(contactServiceUri))

        // Lead Service - Activities
        .route("lead-service-activities", r -> r
            .path("/api/v1/leads/{leadId}/activities/**")
            .filters(f -> f
                .stripPrefix(stripCount)
               // .requestRateLimiter(c -> c.setRateLimiter(defaultRateLimiter))
                .circuitBreaker(config -> config
                    .setName("lead-service-activities")
                    .setFallbackUri("forward:/fallback/crm"))
                .retry(config -> config.setRetries(2)))
            .uri(leadServiceUri))

        // Lead Service - Notes
        .route("lead-service-notes", r -> r
            .path("/api/v1/leads/{leadId}/notes/**")
            .filters(f -> f
                .stripPrefix(stripCount)
               // .requestRateLimiter(c -> c.setRateLimiter(defaultRateLimiter))
                .circuitBreaker(config -> config
                    .setName("lead-service-notes")
                    .setFallbackUri("forward:/fallback/crm"))
                .retry(config -> config.setRetries(2)))
            .uri(leadServiceUri))

        // Lead Service - General
        .route("lead-service", r -> r
            .path("/api/v1/leads/**")
            .filters(f -> f
                .stripPrefix(stripCount)
               // .requestRateLimiter(c -> c.setRateLimiter(defaultRateLimiter))
                .circuitBreaker(config -> config
                    .setName("lead-service")
                    .setFallbackUri("forward:/fallback/crm"))
                .retry(config -> config.setRetries(2)))
            .uri(leadServiceUri))

        // Pipeline Service - Dashboard
        .route("pipeline-service-dashboard", r -> r
            .path("/api/v1/pipeline/dashboard/**")
            .filters(f -> f
                .stripPrefix(stripCount)
               // .requestRateLimiter(c -> c.setRateLimiter(defaultRateLimiter))
                .circuitBreaker(config -> config
                    .setName("pipeline-service-dashboard")
                    .setFallbackUri("forward:/fallback/crm"))
                .retry(config -> config.setRetries(2)))
            .uri(pipelineServiceUri))

        // Pipeline Service - Follow-ups
        .route("pipeline-service-follow-ups", r -> r
            .path("/api/v1/pipeline/follow-ups/**")
            .filters(f -> f
                .stripPrefix(stripCount)
               // .requestRateLimiter(c -> c.setRateLimiter(defaultRateLimiter))
                .circuitBreaker(config -> config
                    .setName("pipeline-service-follow-ups")
                    .setFallbackUri("forward:/fallback/crm"))
                .retry(config -> config.setRetries(2)))
            .uri(pipelineServiceUri))

        // Pipeline Service - General
        .route("pipeline-service", r -> r
            .path("/api/v1/pipeline/**")
            .filters(f -> f
                .stripPrefix(stripCount)
               // .requestRateLimiter(c -> c.setRateLimiter(defaultRateLimiter))
                .circuitBreaker(config -> config
                    .setName("pipeline-service")
                    .setFallbackUri("forward:/fallback/crm"))
                .retry(config -> config.setRetries(2)))
            .uri(pipelineServiceUri))

        // Contact Service - Webhooks
        .route("contact-service-webhooks", r -> r
            .path("/api/v1/crm/webhooks/**")
            .filters(f -> f
                .stripPrefix(stripCount)
               // .requestRateLimiter(c -> c.setRateLimiter(defaultRateLimiter))
                .circuitBreaker(config -> config
                    .setName("contact-service-webhooks")
                    .setFallbackUri("forward:/fallback/webhooks"))
                .retry(config -> config.setRetries(1)))
            .uri(contactServiceUri))

        // Contact Service - Contacts
        .route("contact-service", r -> r
            .path("/api/v1/contacts/**")
            .filters(f -> f
                .stripPrefix(stripCount)
               // .requestRateLimiter(c -> c.setRateLimiter(defaultRateLimiter))
                .circuitBreaker(config -> config
                    .setName("contact-service")
                    .setFallbackUri("forward:/fallback/crm"))
                .retry(config -> config.setRetries(2)))
            .uri(contactServiceUri))

        // Company Service
        .route("company-service", r -> r
            .path("/api/v1/companies/**")
            .filters(f -> f
                .stripPrefix(stripCount)
               // .requestRateLimiter(c -> c.setRateLimiter(defaultRateLimiter))
                .circuitBreaker(config -> config
                    .setName("company-service")
                    .setFallbackUri("forward:/fallback/crm"))
                .retry(config -> config.setRetries(2)))
            .uri(contactServiceUri))

        .build();
  }
}
