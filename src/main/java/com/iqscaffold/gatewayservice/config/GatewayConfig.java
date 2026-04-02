package com.iqscaffold.gatewayservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

/**
 * Gateway configuration class.
 */
@Configuration
public class GatewayConfig {

  private static final Logger logger = LoggerFactory.getLogger(GatewayConfig.class);

  private final IqScaffoldProperties systemProperties;

  public GatewayConfig(final IqScaffoldProperties systemProperties) {
    this.systemProperties = systemProperties;
    logGatewayConfiguration();
  }

  private void logGatewayConfiguration() {
    var routing = systemProperties.gateway().routing();
    var services = routing.services();

    logger.info("Gateway Configuration:");
    logger.info("  API Prefix: {}", routing.apiPrefix().prefix());
    logger.info("  Strip Count: {}", routing.apiPrefix().stripCount());

    services.forEach((name, config) -> {
      if (config.enabled()) {
        logger.info("  Service '{}' enabled: {} -> {}", name, config.path(), config.uri());
      } else {
        logger.info("  Service '{}' disabled", name);
      }
    });

    logger.info("  CORS: {}", systemProperties.gateway().cors().enabled() ? "enabled" : "disabled");
  }
}
