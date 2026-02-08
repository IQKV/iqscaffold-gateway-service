package com.iqscaffold.gatewayservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Unit tests for FeatureValidationService.
 */
@ExtendWith(MockitoExtension.class)
class FeatureValidationServiceTest {

  @Mock
  private WebClient.Builder webClientBuilder;

  @Mock
  private WebClient webClient;

  @Mock
  private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

  @Mock
  private WebClient.RequestHeadersSpec requestHeadersSpec;

  @Mock
  private WebClient.ResponseSpec responseSpec;

  @Mock
  private FeatureUsageTrackingService usageTrackingService;

  private FeatureValidationService service;

  @BeforeEach
  void setUp() {
    when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
    when(webClientBuilder.build()).thenReturn(webClient);

    service = new FeatureValidationService(webClientBuilder, usageTrackingService);
  }

  @Test
  @DisplayName("Should validate feature access successfully when all features are available")
  void shouldValidateFeatureAccessSuccessfully() {
    // Arrange
    var tenantId = "tenant-123";
    var requiredFeatures = Set.of("crm", "billing");
    var endpoint = "/api/v1/crm/contacts";

    var featureContext = new FeatureValidationService.FeatureContext(tenantId);
    featureContext.setPlanId("plan-premium");
    featureContext.setPlanName("Premium Plan");
    featureContext.setEnabledFeatures(Set.of("crm", "billing", "analytics"));

    when(webClient.get()).thenReturn(requestHeadersUriSpec);
    when(requestHeadersUriSpec.uri(anyString(), eq(tenantId))).thenReturn(requestHeadersSpec);
    when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.bodyToMono(FeatureValidationService.FeatureContext.class))
        .thenReturn(Mono.just(featureContext));

    // Act
    var result = service.validateFeatureAccess(tenantId, requiredFeatures, endpoint);

    // Assert
    StepVerifier.create(result)
        .assertNext(validationResult -> {
          assertThat(validationResult.isAllowed()).isTrue();
          assertThat(validationResult.getMissingFeatures()).isEmpty();
          assertThat(validationResult.getFeatureContext()).isNotNull();
          assertThat(validationResult.getFeatureContext().getPlanName()).isEqualTo("Premium Plan");
        })
        .verifyComplete();

    verify(usageTrackingService, times(2)).recordFeatureUsage(eq(tenantId), anyString(), eq(endpoint));
  }

  @Test
  @DisplayName("Should deny access when required features are missing")
  void shouldDenyAccessWhenFeaturesMissing() {
    // Arrange
    var tenantId = "tenant-456";
    var requiredFeatures = Set.of("crm", "billing", "analytics");
    var endpoint = "/api/v1/analytics/reports";

    var featureContext = new FeatureValidationService.FeatureContext(tenantId);
    featureContext.setEnabledFeatures(Set.of("crm"));

    when(webClient.get()).thenReturn(requestHeadersUriSpec);
    when(requestHeadersUriSpec.uri(anyString(), eq(tenantId))).thenReturn(requestHeadersSpec);
    when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.bodyToMono(FeatureValidationService.FeatureContext.class))
        .thenReturn(Mono.just(featureContext));

    // Act
    var result = service.validateFeatureAccess(tenantId, requiredFeatures, endpoint);

    // Assert
    StepVerifier.create(result)
        .assertNext(validationResult -> {
          assertThat(validationResult.isAllowed()).isFalse();
          assertThat(validationResult.getMissingFeatures()).containsExactlyInAnyOrder("billing", "analytics");
        })
        .verifyComplete();

    verify(usageTrackingService, never()).recordFeatureUsage(anyString(), anyString(), anyString());
  }

  @Test
  @DisplayName("Should handle billing service error gracefully")
  void shouldHandleBillingServiceError() {
    // Arrange
    var tenantId = "tenant-789";
    var requiredFeatures = Set.of("crm");
    var endpoint = "/api/v1/crm/leads";

    when(webClient.get()).thenReturn(requestHeadersUriSpec);
    when(requestHeadersUriSpec.uri(anyString(), eq(tenantId))).thenReturn(requestHeadersSpec);
    when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.bodyToMono(FeatureValidationService.FeatureContext.class))
        .thenReturn(Mono.error(new RuntimeException("Service unavailable")));

    // Act
    var result = service.validateFeatureAccess(tenantId, requiredFeatures, endpoint);

    // Assert
    StepVerifier.create(result)
        .assertNext(validationResult -> {
          assertThat(validationResult.isAllowed()).isFalse();
          assertThat(validationResult.getMissingFeatures()).containsExactly("crm");
          assertThat(validationResult.getFeatureContext().getTenantId()).isEqualTo(tenantId);
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should validate quota-based features")
  void shouldValidateQuotaBasedFeatures() {
    // Arrange
    var tenantId = "tenant-quota";
    var requiredFeatures = Set.of("api_calls");
    var endpoint = "/api/v1/data";

    var featureContext = new FeatureValidationService.FeatureContext(tenantId);
    featureContext.setQuotas(Map.of("api_calls", 1000L));

    when(webClient.get()).thenReturn(requestHeadersUriSpec);
    when(requestHeadersUriSpec.uri(anyString(), eq(tenantId))).thenReturn(requestHeadersSpec);
    when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.bodyToMono(FeatureValidationService.FeatureContext.class))
        .thenReturn(Mono.just(featureContext));

    // Act
    var result = service.validateFeatureAccess(tenantId, requiredFeatures, endpoint);

    // Assert
    StepVerifier.create(result)
        .assertNext(validationResult -> {
          assertThat(validationResult.isAllowed()).isTrue();
          assertThat(validationResult.getMissingFeatures()).isEmpty();
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should validate limit-based features")
  void shouldValidateLimitBasedFeatures() {
    // Arrange
    var tenantId = "tenant-limit";
    var requiredFeatures = Set.of("max_users");
    var endpoint = "/api/v1/users";

    var featureContext = new FeatureValidationService.FeatureContext(tenantId);
    featureContext.setLimits(Map.of("max_users", 50L));

    when(webClient.get()).thenReturn(requestHeadersUriSpec);
    when(requestHeadersUriSpec.uri(anyString(), eq(tenantId))).thenReturn(requestHeadersSpec);
    when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.bodyToMono(FeatureValidationService.FeatureContext.class))
        .thenReturn(Mono.just(featureContext));

    // Act
    var result = service.validateFeatureAccess(tenantId, requiredFeatures, endpoint);

    // Assert
    StepVerifier.create(result)
        .assertNext(validationResult -> {
          assertThat(validationResult.isAllowed()).isTrue();
          assertThat(validationResult.getMissingFeatures()).isEmpty();
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should validate tier-based features")
  void shouldValidateTierBasedFeatures() {
    // Arrange
    var tenantId = "tenant-tier";
    var requiredFeatures = Set.of("support_level");
    var endpoint = "/api/v1/support";

    var featureContext = new FeatureValidationService.FeatureContext(tenantId);
    featureContext.setTiers(Map.of("support_level", "premium"));

    when(webClient.get()).thenReturn(requestHeadersUriSpec);
    when(requestHeadersUriSpec.uri(anyString(), eq(tenantId))).thenReturn(requestHeadersSpec);
    when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.bodyToMono(FeatureValidationService.FeatureContext.class))
        .thenReturn(Mono.just(featureContext));

    // Act
    var result = service.validateFeatureAccess(tenantId, requiredFeatures, endpoint);

    // Assert
    StepVerifier.create(result)
        .assertNext(validationResult -> {
          assertThat(validationResult.isAllowed()).isTrue();
          assertThat(validationResult.getMissingFeatures()).isEmpty();
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should deny access for zero quota")
  void shouldDenyAccessForZeroQuota() {
    // Arrange
    var tenantId = "tenant-zero-quota";
    var requiredFeatures = Set.of("api_calls");
    var endpoint = "/api/v1/data";

    var featureContext = new FeatureValidationService.FeatureContext(tenantId);
    featureContext.setQuotas(Map.of("api_calls", 0L));

    when(webClient.get()).thenReturn(requestHeadersUriSpec);
    when(requestHeadersUriSpec.uri(anyString(), eq(tenantId))).thenReturn(requestHeadersSpec);
    when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.bodyToMono(FeatureValidationService.FeatureContext.class))
        .thenReturn(Mono.just(featureContext));

    // Act
    var result = service.validateFeatureAccess(tenantId, requiredFeatures, endpoint);

    // Assert
    StepVerifier.create(result)
        .assertNext(validationResult -> {
          assertThat(validationResult.isAllowed()).isFalse();
          assertThat(validationResult.getMissingFeatures()).contains("api_calls");
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("FeatureContext should calculate enabled feature count correctly")
  void featureContextShouldCalculateEnabledFeatureCount() {
    // Arrange
    var featureContext = new FeatureValidationService.FeatureContext("tenant-123");
    featureContext.setEnabledFeatures(Set.of("crm", "billing"));
    featureContext.setQuotas(Map.of("api_calls", 1000L));
    featureContext.setLimits(Map.of("users", 50L));
    featureContext.setTiers(Map.of("support", "premium"));

    // Act
    var count = featureContext.getEnabledFeatureCount();

    // Assert
    assertThat(count).isEqualTo(5);
  }

  @Test
  @DisplayName("FeatureContext empty should return empty context")
  void featureContextEmptyShouldReturnEmptyContext() {
    // Arrange & Act
    var emptyContext = FeatureValidationService.FeatureContext.empty("tenant-empty");

    // Assert
    assertThat(emptyContext.getTenantId()).isEqualTo("tenant-empty");
    assertThat(emptyContext.getEnabledFeatures()).isEmpty();
    assertThat(emptyContext.getQuotas()).isEmpty();
    assertThat(emptyContext.getLimits()).isEmpty();
    assertThat(emptyContext.getTiers()).isEmpty();
    assertThat(emptyContext.getEnabledFeatureCount()).isZero();
  }

  @Test
  @DisplayName("ValidationResult toString should include relevant information")
  void validationResultToStringShouldIncludeRelevantInfo() {
    // Arrange
    var featureContext = new FeatureValidationService.FeatureContext("tenant-123");
    featureContext.setPlanName("Premium Plan");

    var validationResult = new FeatureValidationService.ValidationResult(
        false, featureContext, Set.of("analytics")
    );

    // Act
    var resultString = validationResult.toString();

    // Assert
    assertThat(resultString).contains("allowed=false");
    assertThat(resultString).contains("missingFeatures=[analytics]");
    assertThat(resultString).contains("planName=Premium Plan");
  }

  @Test
  @DisplayName("FeatureContext should handle null collections gracefully")
  void featureContextShouldHandleNullCollections() {
    // Arrange
    var featureContext = new FeatureValidationService.FeatureContext("tenant-123");
    featureContext.setEnabledFeatures(null);
    featureContext.setQuotas(null);
    featureContext.setLimits(null);
    featureContext.setTiers(null);

    // Act & Assert
    assertThat(featureContext.getEnabledFeatures()).isEmpty();
    assertThat(featureContext.getQuotas()).isEmpty();
    assertThat(featureContext.getLimits()).isEmpty();
    assertThat(featureContext.getTiers()).isEmpty();
  }
}
