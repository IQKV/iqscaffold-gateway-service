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
 * CRM Services rate limiting configuration.
 *
 * <p>Provides programmatic rate limiting policies for all CRM-related endpoints including:
 * <ul>
 *   <li>Lead Service - Lead management and tracking</li>
 *   <li>Pipeline Service - Sales pipeline and follow-up management</li>
 *   <li>Contact Service - Contact and company management</li>
 * </ul>
 *
 * <p>This configuration is only active when the 'crm' profile is enabled.
 *
 * <p>To enable: Add 'crm' to spring.profiles.active
 */
@Configuration
@Profile("crm")
@ConfigurationProperties(prefix = "iqscaffold.gateway.rate-limiting.policies")
public class CrmRateLimitConfig {

  private static final Logger log = LoggerFactory.getLogger(CrmRateLimitConfig.class);

  /**
   * Configures CRM-specific rate limiting policies.
   * These policies override or extend the default rate limiting configuration.
   */
  @Bean
  public Map<String, EndpointPolicyProperties> crmRateLimitPolicies() {
    var policies = new HashMap<String, EndpointPolicyProperties>();

    // Lead Service Rate Limiting - Standard limits for lead operations
    policies.put("/api/v1/leads/**",
        new EndpointPolicyProperties(100, 150, true));
    policies.put("/api/v1/leads/*/activities/**",
        new EndpointPolicyProperties(100, 150, true));
    policies.put("/api/v1/leads/*/notes/**",
        new EndpointPolicyProperties(100, 150, true));

    // Pipeline Service Rate Limiting - Standard limits with lower dashboard limits
    policies.put("/api/v1/pipeline/**",
        new EndpointPolicyProperties(100, 150, true));
    policies.put("/api/v1/pipeline/dashboard/**",
        new EndpointPolicyProperties(50, 100, true));
    policies.put("/api/v1/pipeline/follow-ups/**",
        new EndpointPolicyProperties(100, 150, true));

    // Contact Service Rate Limiting - Standard limits
    policies.put("/api/v1/contacts/**",
        new EndpointPolicyProperties(100, 150, true));

    // Company Service Rate Limiting - Standard limits
    policies.put("/api/v1/companies/**",
        new EndpointPolicyProperties(100, 150, true));

    // Contact Service Webhook Management - Moderate limits for admin operations
    policies.put("/api/v1/crm/webhooks",
        new EndpointPolicyProperties(30, 50, true));
    policies.put("/api/v1/crm/webhooks/**",
        new EndpointPolicyProperties(40, 60, true));

    log.info("Initialized {} CRM-specific rate limiting policies", policies.size());

    return policies;
  }
}
