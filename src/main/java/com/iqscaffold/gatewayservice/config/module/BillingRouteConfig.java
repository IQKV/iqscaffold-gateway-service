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
 * Gateway route configuration for Billing Service endpoints.
 * Active when the 'billing' profile is enabled.
 */
@Configuration
@Profile("billing")
@ConditionalOnProperty(name = "spring.cloud.gateway.enabled", havingValue = "true", matchIfMissing = true)
public class BillingRouteConfig {

  private static final Logger log = LoggerFactory.getLogger(BillingRouteConfig.class);

  private final IqScaffoldProperties properties;

  public BillingRouteConfig(final IqScaffoldProperties properties) {
    this.properties = properties;
  }

  @Bean
  public RouteLocator billingServiceRoutes(RouteLocatorBuilder builder) {
    var stripCount = properties.gateway().routing().apiPrefix().stripCount();
    var billingServiceUri = properties.gateway().routing().services().get("billing-service").uri();

    log.info("Configuring Billing routes");

    return builder.routes()

        .route("billing-service-health", r -> r
            .path("/api/v1/billing/*/health")
            .filters(f -> f.stripPrefix(stripCount))
            .uri(billingServiceUri))

        .route("billing-service-features-my-features", r -> r
            .path("/api/v1/features/my-features")
            .filters(f -> f.stripPrefix(stripCount))
            .uri(billingServiceUri))

        .route("billing-service-features-enabled", r -> r
            .path("/api/v1/features/enabled")
            .filters(f -> f.stripPrefix(stripCount))
            .uri(billingServiceUri))

        .route("billing-service-subscriptions", r -> r
            .path("/api/v1/billing/subscriptions/**")
            .filters(f -> f.stripPrefix(stripCount))
            .uri(billingServiceUri))

        .route("billing-service-payments", r -> r
            .path("/api/v1/billing/payments/**")
            .filters(f -> f.stripPrefix(stripCount))
            .uri(billingServiceUri))

        .route("billing-service-invoices", r -> r
            .path("/api/v1/billing/invoices/**")
            .filters(f -> f.stripPrefix(stripCount))
            .uri(billingServiceUri))

        .route("billing-service-payouts", r -> r
            .path("/api/v1/billing/payouts/**")
            .filters(f -> f.stripPrefix(stripCount))
            .uri(billingServiceUri))

        .route("billing-service-subscription-plans", r -> r
            .path("/api/v1/billing/subscription-plans/**")
            .filters(f -> f.stripPrefix(stripCount))
            .uri(billingServiceUri))

        .route("billing-service-admin", r -> r
            .path("/api/v1/admin/billing/**")
            .filters(f -> f.stripPrefix(stripCount))
            .uri(billingServiceUri))

        .route("billing-service-webhooks", r -> r
            .path("/api/v1/billing/webhooks/**")
            .filters(f -> f.stripPrefix(stripCount))
            .uri(billingServiceUri))

        .route("billing-service-internal", r -> r
            .path("/api/v1/internal/features/**")
            .filters(f -> f.stripPrefix(stripCount))
            .uri(billingServiceUri))

        .build();
  }
}
