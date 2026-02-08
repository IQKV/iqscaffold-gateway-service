package com.iqscaffold.gatewayservice.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * Unit tests for FallbackController.
 */
class FallbackControllerTest {

  private FallbackController controller;

  @BeforeEach
  void setUp() {
    controller = new FallbackController();
  }

  @Test
  @DisplayName("Should return features fallback response with SERVICE_UNAVAILABLE status")
  void shouldReturnFeaturesFallbackResponse() {
    // Act
    var response = controller.featuresFallback();

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(response.getHeaders().getFirst("X-Fallback-Response")).isEqualTo("true");

    var body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body).containsKey("enabledFeatures");
    assertThat(body).containsKey("allFeatures");
    assertThat(body).containsKey("planName");
    assertThat(body).containsKey("subscriptionStatus");
    assertThat(body).containsKey("tenantId");
    assertThat(body).containsKey("error");
    assertThat(body).containsKey("timestamp");

    assertThat(body.get("planName")).isEqualTo("Service Unavailable");
    assertThat(body.get("subscriptionStatus")).isEqualTo("UNKNOWN");
    assertThat(body.get("tenantId")).isEqualTo("unknown");
    assertThat(body.get("error")).isEqualTo("Feature service temporarily unavailable");
  }

  @Test
  @DisplayName("Should return billing GET fallback response with SERVICE_UNAVAILABLE status")
  void shouldReturnBillingGetFallbackResponse() {
    // Act
    var response = controller.billingFallback();

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(response.getHeaders().getFirst("X-Fallback-Response")).isEqualTo("true");

    var body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body).containsKey("error");
    assertThat(body).containsKey("message");
    assertThat(body).containsKey("timestamp");
    assertThat(body).containsKey("status");

    assertThat(body.get("error")).isEqualTo("Billing service temporarily unavailable");
    assertThat(body.get("message")).isEqualTo("Please try again later");
    assertThat(body.get("status")).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
  }

  @Test
  @DisplayName("Should return webhooks fallback response with ACCEPTED status")
  void shouldReturnWebhooksFallbackResponse() {
    // Act
    var response = controller.webhooksFallback();

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    assertThat(response.getHeaders().getFirst("X-Fallback-Response")).isEqualTo("true");

    var body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body).containsKey("message");
    assertThat(body).containsKey("status");
    assertThat(body).containsKey("timestamp");

    assertThat(body.get("message")).isEqualTo("Webhook received but service temporarily unavailable");
    assertThat(body.get("status")).isEqualTo("queued");
  }

  @Test
  @DisplayName("Should return internal GET fallback response with SERVICE_UNAVAILABLE status")
  void shouldReturnInternalGetFallbackResponse() {
    // Act
    var response = controller.internalFallback();

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(response.getHeaders().getFirst("X-Fallback-Response")).isEqualTo("true");
    assertThat(response.getHeaders().getFirst("X-Internal-Fallback")).isEqualTo("true");

    var body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body).containsKey("enabled");
    assertThat(body).containsKey("features");
    assertThat(body).containsKey("error");
    assertThat(body).containsKey("timestamp");

    assertThat(body.get("enabled")).isEqualTo(false);
    assertThat(body.get("error")).isEqualTo("Internal service temporarily unavailable");
  }

  @Test
  @DisplayName("Features fallback should return empty lists")
  void featuresFallbackShouldReturnEmptyLists() {
    // Act
    var response = controller.featuresFallback();
    var body = response.getBody();

    // Assert
    assertThat(body).isNotNull();
    assertThat(body.get("enabledFeatures")).asList().isEmpty();
    assertThat(body.get("allFeatures")).asList().isEmpty();
  }

  @Test
  @DisplayName("Internal fallback should return empty features list")
  void internalFallbackShouldReturnEmptyFeaturesList() {
    // Act
    var response = controller.internalFallback();
    var body = response.getBody();

    // Assert
    assertThat(body).isNotNull();
    assertThat(body.get("features")).asList().isEmpty();
  }

  @Test
  @DisplayName("All fallback responses should include timestamp")
  void allFallbackResponsesShouldIncludeTimestamp() {
    // Act
    var featuresResponse = controller.featuresFallback();
    var billingResponse = controller.billingFallback();
    var webhooksResponse = controller.webhooksFallback();
    var internalResponse = controller.internalFallback();

    // Assert
    assertThat(featuresResponse.getBody()).containsKey("timestamp");
    assertThat(billingResponse.getBody()).containsKey("timestamp");
    assertThat(webhooksResponse.getBody()).containsKey("timestamp");
    assertThat(internalResponse.getBody()).containsKey("timestamp");
  }

  @Test
  @DisplayName("Webhooks fallback should prevent retries with ACCEPTED status")
  void webhooksFallbackShouldPreventRetries() {
    // Act
    var response = controller.webhooksFallback();

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    assertThat(response.getBody().get("status")).isEqualTo("queued");
  }

  @Test
  @DisplayName("Billing fallback should include numeric status code")
  void billingFallbackShouldIncludeNumericStatusCode() {
    // Act
    var response = controller.billingFallback();

    // Assert
    assertThat(response.getBody().get("status")).isEqualTo(503);
  }

  @Test
  @DisplayName("Internal fallback should have both fallback headers")
  void internalFallbackShouldHaveBothFallbackHeaders() {
    // Act
    var response = controller.internalFallback();

    // Assert
    assertThat(response.getHeaders().getFirst("X-Fallback-Response")).isEqualTo("true");
    assertThat(response.getHeaders().getFirst("X-Internal-Fallback")).isEqualTo("true");
  }
}
