package com.iqscaffold.gatewayservice.config.module;

import java.util.HashMap;
import java.util.Map;

import com.iqscaffold.gatewayservice.config.IqScaffoldProperties.GatewayProperties.RateLimitingProperties.PoliciesProperties.EndpointPolicyProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Billing Service rate limiting configuration.
 *
 * <p>Provides programmatic rate limiting policies for all billing-related endpoints.
 * This configuration is only active when the 'billing' profile is enabled.
 *
 * <p>Rate limiting policies include:
 * <ul>
 *   <li>Payment operations - Strict limits for payment processing</li>
 *   <li>Subscription operations - Moderate limits for subscription management</li>
 *   <li>Invoice operations - Standard limits for invoice access</li>
 *   <li>Payout operations - Moderate limits for payout management</li>
 *   <li>Admin operations - Lower limits for administrative tasks</li>
 *   <li>Webhook operations - High limits for external provider webhooks</li>
 * </ul>
 *
 * <p>To enable: Add 'billing' to spring.profiles.active
 */
@Configuration
@Profile("billing")
@ConfigurationProperties(prefix = "iqscaffold.gateway.rate-limiting.policies")
public class BillingRateLimitConfig {

  private static final Logger log = LoggerFactory.getLogger(BillingRateLimitConfig.class);

  /**
   * Configures billing-specific rate limiting policies.
   * These policies override or extend the default rate limiting configuration.
   */
  @Bean
  public Map<String, EndpointPolicyProperties> billingRateLimitPolicies() {
    var policies = new HashMap<String, EndpointPolicyProperties>();

    // Payment Operations - Strict limits to prevent abuse
    policies.put("/api/v1/billing/payments/intent",
        new EndpointPolicyProperties(30, 50, true));
    policies.put("/api/v1/billing/payments/**",
        new EndpointPolicyProperties(60, 100, true));
    policies.put("/api/v1/billing/payments/*/refund",
        new EndpointPolicyProperties(10, 20, true));

    // Subscription Operations - Moderate limits
    policies.put("/api/v1/billing/subscriptions",
        new EndpointPolicyProperties(40, 60, true));
    policies.put("/api/v1/billing/subscriptions/**",
        new EndpointPolicyProperties(50, 75, true));
    policies.put("/api/v1/billing/subscriptions/*/cancel",
        new EndpointPolicyProperties(10, 15, true));
    policies.put("/api/v1/billing/subscriptions/*/pause",
        new EndpointPolicyProperties(10, 15, true));
    policies.put("/api/v1/billing/subscriptions/*/resume",
        new EndpointPolicyProperties(10, 15, true));

    // Subscription Plan Management - Standard limits
    policies.put("/api/v1/billing/subscription-plans",
        new EndpointPolicyProperties(30, 50, true));
    policies.put("/api/v1/billing/subscription-plans/**",
        new EndpointPolicyProperties(40, 60, true));

    // Invoice Operations - Standard limits
    policies.put("/api/v1/billing/invoices",
        new EndpointPolicyProperties(50, 75, true));
    policies.put("/api/v1/billing/invoices/**",
        new EndpointPolicyProperties(40, 60, true));

    // Payout Operations - Moderate limits
    policies.put("/api/v1/billing/payouts",
        new EndpointPolicyProperties(30, 50, true));
    policies.put("/api/v1/billing/payouts/**",
        new EndpointPolicyProperties(40, 60, true));

    // Admin Billing Operations - Lower limits for administrative tasks
    policies.put("/api/v1/admin/billing/merchants/**",
        new EndpointPolicyProperties(20, 30, true));
    policies.put("/api/v1/admin/billing/gateway-config",
        new EndpointPolicyProperties(15, 25, true));
    policies.put("/api/v1/admin/billing/gateway-config/**",
        new EndpointPolicyProperties(20, 30, true));

    // Webhook Operations - High limits for external providers, no tenant quotas
    policies.put("/api/v1/billing/webhooks/**",
        new EndpointPolicyProperties(200, 500, false));

    log.info("Initialized {} billing-specific rate limiting policies", policies.size());

    return policies;
  }
}
