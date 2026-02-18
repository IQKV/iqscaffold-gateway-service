package com.iqscaffold.gatewayservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration for WebClient.
 * <p>
 * In Spring Boot 4.0, WebClient.Builder is no longer auto-configured by default.
 * This configuration provides the WebClient.Builder bean required by services
 * that need to make HTTP calls to other microservices.
 */
@Configuration
public class WebClientConfig {

  /**
   * Provides a WebClient.Builder bean for dependency injection.
   * <p>
   * Services can inject this builder and customize it with base URLs,
   * filters, and other configurations as needed.
   *
   * @return a new WebClient.Builder instance
   */
  @Bean
  public WebClient.Builder webClientBuilder() {
    return WebClient.builder();
  }
}
