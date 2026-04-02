package com.iqscaffold.gatewayservice.config.module;

import com.iqscaffold.gatewayservice.config.IqScaffoldProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Gateway route configuration for CRM Services.
 * Active when the 'crm' profile is enabled.
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

    log.info("Configuring CRM routes");

    return builder.routes()

        .route("lead-service-health", r -> r
            .path("/api/v1/leads/health")
            .filters(f -> f.stripPrefix(stripCount))
            .uri(leadServiceUri))

        .route("pipeline-service-health", r -> r
            .path("/api/v1/pipeline/health")
            .filters(f -> f.stripPrefix(stripCount))
            .uri(pipelineServiceUri))

        .route("contact-service-health", r -> r
            .path("/api/v1/contacts/health")
            .filters(f -> f.stripPrefix(stripCount))
            .uri(contactServiceUri))

        .route("lead-service-activities", r -> r
            .path("/api/v1/leads/{leadId}/activities/**")
            .filters(f -> f.stripPrefix(stripCount))
            .uri(leadServiceUri))

        .route("lead-service-notes", r -> r
            .path("/api/v1/leads/{leadId}/notes/**")
            .filters(f -> f.stripPrefix(stripCount))
            .uri(leadServiceUri))

        .route("lead-service", r -> r
            .path("/api/v1/leads/**")
            .filters(f -> f.stripPrefix(stripCount))
            .uri(leadServiceUri))

        .route("pipeline-service-dashboard", r -> r
            .path("/api/v1/pipeline/dashboard/**")
            .filters(f -> f.stripPrefix(stripCount))
            .uri(pipelineServiceUri))

        .route("pipeline-service-follow-ups", r -> r
            .path("/api/v1/pipeline/follow-ups/**")
            .filters(f -> f.stripPrefix(stripCount))
            .uri(pipelineServiceUri))

        .route("pipeline-service", r -> r
            .path("/api/v1/pipeline/**")
            .filters(f -> f.stripPrefix(stripCount))
            .uri(pipelineServiceUri))

        .route("contact-service-webhooks", r -> r
            .path("/api/v1/crm/webhooks/**")
            .filters(f -> f.stripPrefix(stripCount))
            .uri(contactServiceUri))

        .route("contact-service", r -> r
            .path("/api/v1/contacts/**")
            .filters(f -> f.stripPrefix(stripCount))
            .uri(contactServiceUri))

        .route("company-service", r -> r
            .path("/api/v1/companies/**")
            .filters(f -> f.stripPrefix(stripCount))
            .uri(contactServiceUri))

        .build();
  }
}
