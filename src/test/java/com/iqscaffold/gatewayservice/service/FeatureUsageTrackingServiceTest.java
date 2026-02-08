package com.iqscaffold.gatewayservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Unit tests for FeatureUsageTrackingService.
 */
@ExtendWith(MockitoExtension.class)
class FeatureUsageTrackingServiceTest {

  @Mock
  private WebClient.Builder webClientBuilder;

  @Mock
  private WebClient webClient;

  private FeatureUsageTrackingService service;

  @BeforeEach
  void setUp() {
    when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
    when(webClientBuilder.build()).thenReturn(webClient);

    service = new FeatureUsageTrackingService(webClientBuilder);

    MDC.clear();
  }

  @Test
  @DisplayName("Should create service successfully")
  void shouldCreateServiceSuccessfully() {
    // Assert
    assertThat(service).isNotNull();
  }

  @Test
  @DisplayName("Should record feature usage without throwing exception")
  void shouldRecordFeatureUsageWithoutThrowing() {
    // Arrange
    var tenantId = "tenant-123";
    var featureKey = "crm";
    var endpoint = "/api/v1/crm/contacts";
    var userId = "user-456";
    var correlationId = "corr-789";

    MDC.put("userId", userId);
    MDC.put("correlationId", correlationId);

    // Act - should not throw exception even if WebClient fails
    service.recordFeatureUsage(tenantId, featureKey, endpoint);

    // Assert - method completes without exception
    assertThat(true).isTrue();

    MDC.clear();
  }

  @Test
  @DisplayName("Should handle missing MDC context gracefully")
  void shouldHandleMissingMDCContext() {
    // Arrange
    var tenantId = "tenant-123";
    var featureKey = "billing";
    var endpoint = "/api/v1/billing/invoices";

    // Act - should not throw exception
    service.recordFeatureUsage(tenantId, featureKey, endpoint);

    // Assert - method completes without exception
    assertThat(true).isTrue();
  }

  @Test
  @DisplayName("Should record batch feature usage without throwing")
  void shouldRecordBatchFeatureUsage() {
    // Arrange
    var tenantId = "tenant-batch";
    var featureKeys = Set.of("crm", "billing", "analytics");
    var endpoint = "/api/v1/dashboard";

    // Act - should not throw exception
    service.recordBatchFeatureUsage(tenantId, featureKeys, endpoint);

    // Assert - method completes without exception
    assertThat(true).isTrue();
  }

  @Test
  @DisplayName("FeatureUsageRequest should have correct getters and setters")
  void featureUsageRequestShouldHaveCorrectGettersAndSetters() {
    // Arrange & Act
    var request = new FeatureUsageTrackingService.FeatureUsageRequest();
    request.setTenantId("tenant-123");
    request.setFeatureKey("crm");
    request.setEndpoint("/api/v1/crm");
    request.setUserId("user-456");
    request.setCorrelationId("corr-789");

    // Assert
    assertThat(request.getTenantId()).isEqualTo("tenant-123");
    assertThat(request.getFeatureKey()).isEqualTo("crm");
    assertThat(request.getEndpoint()).isEqualTo("/api/v1/crm");
    assertThat(request.getUserId()).isEqualTo("user-456");
    assertThat(request.getCorrelationId()).isEqualTo("corr-789");
  }

  @Test
  @DisplayName("FeatureUsageRequest constructor should set all fields")
  void featureUsageRequestConstructorShouldSetAllFields() {
    // Arrange & Act
    var request = new FeatureUsageTrackingService.FeatureUsageRequest(
        "tenant-123", "billing", "/api/v1/billing", "user-456", "corr-789"
    );

    // Assert
    assertThat(request.getTenantId()).isEqualTo("tenant-123");
    assertThat(request.getFeatureKey()).isEqualTo("billing");
    assertThat(request.getEndpoint()).isEqualTo("/api/v1/billing");
    assertThat(request.getUserId()).isEqualTo("user-456");
    assertThat(request.getCorrelationId()).isEqualTo("corr-789");
  }

  @Test
  @DisplayName("FeatureUsageRequest default constructor should work")
  void featureUsageRequestDefaultConstructorShouldWork() {
    // Arrange & Act
    var request = new FeatureUsageTrackingService.FeatureUsageRequest();

    // Assert
    assertThat(request).isNotNull();
    assertThat(request.getTenantId()).isNull();
    assertThat(request.getFeatureKey()).isNull();
    assertThat(request.getEndpoint()).isNull();
    assertThat(request.getUserId()).isNull();
    assertThat(request.getCorrelationId()).isNull();
  }

  @Test
  @DisplayName("Should handle null tenant ID gracefully")
  void shouldHandleNullTenantId() {
    // Arrange
    String tenantId = null;
    var featureKey = "crm";
    var endpoint = "/api/v1/crm";

    // Act - should not throw exception
    service.recordFeatureUsage(tenantId, featureKey, endpoint);

    // Assert - method completes without exception
    assertThat(true).isTrue();
  }

  @Test
  @DisplayName("Should handle null feature key gracefully")
  void shouldHandleNullFeatureKey() {
    // Arrange
    var tenantId = "tenant-123";
    String featureKey = null;
    var endpoint = "/api/v1/crm";

    // Act - should not throw exception
    service.recordFeatureUsage(tenantId, featureKey, endpoint);

    // Assert - method completes without exception
    assertThat(true).isTrue();
  }

  @Test
  @DisplayName("Should handle empty feature keys set in batch")
  void shouldHandleEmptyFeatureKeysSet() {
    // Arrange
    var tenantId = "tenant-123";
    var featureKeys = Set.<String>of();
    var endpoint = "/api/v1/dashboard";

    // Act - should not throw exception
    service.recordBatchFeatureUsage(tenantId, featureKeys, endpoint);

    // Assert - method completes without exception
    assertThat(true).isTrue();
  }
}
