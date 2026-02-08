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
  public RouteLocator crmRoutes(final RouteLocatorBuilder builder) {
    var stripCount = properties.gateway().routing().apiPrefix().stripCount();
    var leadServiceUri = properties.gateway().routing().services().get("lead-service").uri();
    var pipelineServiceUri = properties.gateway().routing().services().get("pipeline-service").uri();
    var contactServiceUri = properties.gateway().routing().services().get("contact-service").uri();

    // Get rate limiting policies from configuration
    var rateLimitPolicies = properties.gateway().rateLimiting().policies().endpoints();
    var defaultReplenishRate = properties.gateway().rateLimiting().policies().defaultRequestsPerMinute();
    var defaultBurstCapacity = properties.gateway().rateLimiting().policies().defaultBurstCapacity();

    log.info("Configuring CRM routes with {} rate limit policies", rateLimitPolicies.size());

    return builder.routes()
        // Lead Service - Activities (specific rate limit)
        .route("lead-service-activities", r -> r
            .path("/api/v1/leads/{leadId}/activities/**")
            .filters(f -> f
                .stripPrefix(stripCount)
                .requestRateLimiter(c -> c.setRateLimiter(
                    getRateLimiter("/api/v1/leads/*/activities/**", rateLimitPolicies, 
                        defaultReplenishRate, defaultBurstCapacity)))
                .circuitBreaker(config -> config
                    .setName("lead-service-activities")
                    .setFallbackUri("forward:/fallback/crm"))
                .retry(config -> config.setRetries(2)))
            .uri(leadServiceUri))

        // Lead Service - Notes (specific rate limit)
        .route("lead-service-notes", r -> r
            .path("/api/v1/leads/{leadId}/notes/**")
            .filters(f -> f
                .stripPrefix(stripCount)
                .requestRateLimiter(c -> c.setRateLimiter(
                    getRateLimiter("/api/v1/leads/*/notes/**", rateLimitPolicies, 
                        defaultReplenishRate, defaultBurstCapacity)))
                .circuitBreaker(config -> config
                    .setName("lead-service-notes")
                    .setFallbackUri("forward:/fallback/crm"))
                .retry(config -> config.setRetries(2)))
            .uri(leadServiceUri))

        // Lead Service - General (specific rate limit)
        .route("lead-service", r -> r
            .path("/api/v1/leads/**")
            .filters(f -> f
                .stripPrefix(stripCount)
                .requestRateLimiter(c -> c.setRateLimiter(
                    getRateLimiter("/api/v1/leads/**", rateLimitPolicies, 
                        defaultReplenishRate, defaultBurstCapacity)))
                .circuitBreaker(config -> config
                    .setName("lead-service")
                    .setFallbackUri("forward:/fallback/crm"))
                .retry(config -> config.setRetries(2)))
            .uri(leadServiceUri))

        // Pipeline Service - Dashboard (specific rate limit - lower)
        .route("pipeline-service-dashboard", r -> r
            .path("/api/v1/pipeline/dashboard/**")
            .filters(f -> f
                .stripPrefix(stripCount)
                .requestRateLimiter(c -> c.setRateLimiter(
                    getRateLimiter("/api/v1/pipeline/dashboard/**", rateLimitPolicies, 
                        defaultReplenishRate, defaultBurstCapacity)))
                .circuitBreaker(config -> config
                    .setName("pipeline-service-dashboard")
                    .setFallbackUri("forward:/fallback/crm"))
                .retry(config -> config.setRetries(2)))
            .uri(pipelineServiceUri))

        // Pipeline Service - Follow-ups (specific rate limit)
        .route("pipeline-service-follow-ups", r -> r
            .path("/api/v1/pipeline/follow-ups/**")
            .filters(f -> f
                .stripPrefix(stripCount)
                .requestRateLimiter(c -> c.setRateLimiter(
                    getRateLimiter("/api/v1/pipeline/follow-ups/**", rateLimitPolicies, 
                        defaultReplenishRate, defaultBurstCapacity)))
                .circuitBreaker(config -> config
                    .setName("pipeline-service-follow-ups")
                    .setFallbackUri("forward:/fallback/crm"))
                .retry(config -> config.setRetries(2)))
            .uri(pipelineServiceUri))

        // Pipeline Service - General (specific rate limit)
        .route("pipeline-service", r -> r
            .path("/api/v1/pipeline/**")
            .filters(f -> f
                .stripPrefix(stripCount)
                .requestRateLimiter(c -> c.setRateLimiter(
                    getRateLimiter("/api/v1/pipeline/**", rateLimitPolicies, 
                        defaultReplenishRate, defaultBurstCapacity)))
                .circuitBreaker(config -> config
                    .setName("pipeline-service")
                    .setFallbackUri("forward:/fallback/crm"))
                .retry(config -> config.setRetries(2)))
            .uri(pipelineServiceUri))

        // Contact Service - Webhooks (specific rate limit)
        .route("contact-service-webhooks", r -> r
            .path("/api/v1/crm/webhooks/**")
            .filters(f -> f
                .stripPrefix(stripCount)
                .requestRateLimiter(c -> c.setRateLimiter(
                    getRateLimiter("/api/v1/crm/webhooks/**", rateLimitPolicies, 
                        defaultReplenishRate, defaultBurstCapacity)))
                .circuitBreaker(config -> config
                    .setName("contact-service-webhooks")
                    .setFallbackUri("forward:/fallback/webhooks"))
                .retry(config -> config.setRetries(1)))
            .uri(contactServiceUri))

        // Contact Service - Contacts (specific rate limit)
        .route("contact-service", r -> r
            .path("/api/v1/contacts/**")
            .filters(f -> f
                .stripPrefix(stripCount)
                .requestRateLimiter(c -> c.setRateLimiter(
                    getRateLimiter("/api/v1/contacts/**", rateLimitPolicies, 
                        defaultReplenishRate, defaultBurstCapacity)))
                .circuitBreaker(config -> config
                    .setName("contact-service")
                    .setFallbackUri("forward:/fallback/crm"))
                .retry(config -> config.setRetries(2)))
            .uri(contactServiceUri))

        // Company Service (specific rate limit)
        .route("company-service", r -> r
            .path("/api/v1/companies/**")
            .filters(f -> f
                .stripPrefix(stripCount)
                .requestRateLimiter(c -> c.setRateLimiter(
                    getRateLimiter("/api/v1/companies/**", rateLimitPolicies, 
                        defaultReplenishRate, defaultBurstCapacity)))
                .circuitBreaker(config -> config
                    .setName("company-service")
                    .setFallbackUri("forward:/fallback/crm"))
                .retry(config -> config.setRetries(2)))
            .uri(contactServiceUri))
        .build();
  }

  /**
   * Gets the appropriate rate limiter for an endpoint.
   * Falls back to default values if no specific policy is configured.
   */
  private RedisRateLimiter getRateLimiter(
      String endpoint,
      java.util.Map<String, IqScaffoldProperties.GatewayProperties.RateLimitingProperties.PoliciesProperties.EndpointPolicyProperties> policies,
      int defaultReplenishRate,
      int defaultBurstCapacity) {

    var policy = policies.get(endpoint);
    if (policy != null) {
      log.debug("Using specific rate limit for {}: {} req/min, {} burst", 
          endpoint, policy.requestsPerMinute(), policy.burstCapacity());
      return new RedisRateLimiter(policy.requestsPerMinute(), policy.burstCapacity());
    }

    log.debug("Using default rate limit for {}: {} req/min, {} burst", 
        endpoint, defaultReplenishRate, defaultBurstCapacity);
    return new RedisRateLimiter(defaultReplenishRate, defaultBurstCapacity);
  }
}
