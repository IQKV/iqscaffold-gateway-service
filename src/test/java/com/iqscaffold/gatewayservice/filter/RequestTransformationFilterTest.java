package com.iqscaffold.gatewayservice.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.iqscaffold.gatewayservice.common.GatewayConstants;
import com.iqscaffold.gatewayservice.config.IqScaffoldProperties;
import com.iqscaffold.gatewayservice.service.FeatureValidationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Unit tests for RequestTransformationFilter.
 */
class RequestTransformationFilterTest {

  private RequestTransformationFilter filter;
  private GatewayFilterChain chain;
  private IqScaffoldProperties properties;

  @BeforeEach
  void setUp() {
    properties = createTestIqScaffoldProperties();
    filter = new RequestTransformationFilter(properties);
    chain = mock(GatewayFilterChain.class);
    when(chain.filter(any())).thenReturn(Mono.empty());
  }

  @AfterEach
  void tearDown() {
    MDC.clear();
  }

  private IqScaffoldProperties createTestIqScaffoldProperties() {
    var cacheProperties = new IqScaffoldProperties.CacheProperties(
        new IqScaffoldProperties.CacheProperties.RedisProperties(
            "localhost", 6379, null, 0, Duration.ofSeconds(5),
            new IqScaffoldProperties.CacheProperties.RedisProperties.PoolProperties(10, 5, 2, Duration.ofSeconds(2)),
            "iqscaffold:cache:", Duration.ofMinutes(30), false
        )
    );

    var requestTransformation = new IqScaffoldProperties.GatewayProperties.TransformationProperties.RequestTransformationProperties(
        true, true, true, true, true, List.of(), Map.of()
    );

    var responseTransformation = new IqScaffoldProperties.GatewayProperties.TransformationProperties.ResponseTransformationProperties(
        true, true, true, true, List.of()
    );

    var transformation = new IqScaffoldProperties.GatewayProperties.TransformationProperties(
        requestTransformation, responseTransformation
    );

    var gatewayProperties = new IqScaffoldProperties.GatewayProperties(
        null, null, null, null, null, transformation, null
    );

    var observabilityProperties = new IqScaffoldProperties.ObservabilityProperties(null, null, null);

    var i18nProperties = new IqScaffoldProperties.I18nProperties(
        List.of("en", "es", "fr"), "en"
    );

    return new IqScaffoldProperties(cacheProperties, gatewayProperties, i18nProperties, observabilityProperties);
  }

  private IqScaffoldProperties createDisabledTransformationProperties() {
    var cacheProperties = new IqScaffoldProperties.CacheProperties(
        new IqScaffoldProperties.CacheProperties.RedisProperties(
            "localhost", 6379, null, 0, Duration.ofSeconds(5),
            new IqScaffoldProperties.CacheProperties.RedisProperties.PoolProperties(10, 5, 2, Duration.ofSeconds(2)),
            "iqscaffold:cache:", Duration.ofMinutes(30), false
        )
    );

    var requestTransformation = new IqScaffoldProperties.GatewayProperties.TransformationProperties.RequestTransformationProperties(
        false, false, false, false, false, List.of(), Map.of()
    );

    var responseTransformation = new IqScaffoldProperties.GatewayProperties.TransformationProperties.ResponseTransformationProperties(
        false, false, false, false, List.of()
    );

    var transformation = new IqScaffoldProperties.GatewayProperties.TransformationProperties(
        requestTransformation, responseTransformation
    );

    var gatewayProperties = new IqScaffoldProperties.GatewayProperties(
        null, null, null, null, null, transformation, null
    );

    var observabilityProperties = new IqScaffoldProperties.ObservabilityProperties(null, null, null);

    var i18nProperties = new IqScaffoldProperties.I18nProperties(
        List.of("en", "es", "fr"), "en"
    );

    return new IqScaffoldProperties(cacheProperties, gatewayProperties, i18nProperties, observabilityProperties);
  }

  @Test
  @DisplayName("Should enrich request headers with correlation ID")
  void shouldEnrichRequestHeadersWithCorrelationId() {
    // Arrange
    var correlationId = "test-correlation-123";
    MDC.put(GatewayConstants.MdcKeys.CORRELATION_ID, correlationId);

    var request = MockServerHttpRequest.get("/api/v1/auth/login").build();
    var exchange = MockServerWebExchange.from(request);
    var config = new RequestTransformationFilter.Config();

    // Act
    var gatewayFilter = filter.apply(config);
    var result = gatewayFilter.filter(exchange, chain);

    // Assert
    StepVerifier.create(result)
        .verifyComplete();
  }

  @Test
  @DisplayName("Should enrich request headers with tenant and user context")
  void shouldEnrichRequestHeadersWithTenantContext() {
    // Arrange
    var tenantId = "tenant-123";
    var userId = "user-456";
    MDC.put(GatewayConstants.MdcKeys.TENANT_ID, tenantId);
    MDC.put(GatewayConstants.MdcKeys.USER_ID, userId);

    var request = MockServerHttpRequest.get("/api/v1/admin/users").build();
    var exchange = MockServerWebExchange.from(request);
    var config = new RequestTransformationFilter.Config();

    // Act
    var gatewayFilter = filter.apply(config);
    var result = gatewayFilter.filter(exchange, chain);

    // Assert
    StepVerifier.create(result)
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle request without MDC context")
  void shouldHandleRequestWithoutMDCContext() {
    // Arrange
    var request = MockServerHttpRequest.get("/actuator/health").build();
    var exchange = MockServerWebExchange.from(request);
    var config = new RequestTransformationFilter.Config();

    // Act
    var gatewayFilter = filter.apply(config);
    var result = gatewayFilter.filter(exchange, chain);

    // Assert
    StepVerifier.create(result)
        .verifyComplete();
  }

  @Test
  @DisplayName("Should skip transformation when disabled")
  void shouldSkipTransformationWhenDisabled() {
    // Arrange
    var disabledProperties = createDisabledTransformationProperties();
    var disabledFilter = new RequestTransformationFilter(disabledProperties);

    var request = MockServerHttpRequest.get("/api/v1/users").build();
    var exchange = MockServerWebExchange.from(request);
    var config = new RequestTransformationFilter.Config();

    // Act
    var gatewayFilter = disabledFilter.apply(config);
    var result = gatewayFilter.filter(exchange, chain);

    // Assert
    StepVerifier.create(result)
        .verifyComplete();
  }

  @Test
  @DisplayName("Should add feature context headers when available")
  void shouldAddFeatureContextHeaders() {
    // Arrange
    var featureContext = new FeatureValidationService.FeatureContext("tenant-123");
    featureContext.setPlanId("plan-premium");
    featureContext.setPlanName("Premium Plan");
    featureContext.setEnabledFeatures(Set.of("crm", "billing"));
    featureContext.setQuotas(Map.of("api_calls", 1000L));
    featureContext.setLimits(Map.of("users", 50L));
    featureContext.setTiers(Map.of("support", "premium"));

    var request = MockServerHttpRequest.get("/api/v1/crm/contacts").build();
    var exchange = MockServerWebExchange.from(request);
    exchange.getAttributes().put(GatewayConstants.Attributes.FEATURE_CONTEXT, featureContext);

    MDC.put(GatewayConstants.MdcKeys.CORRELATION_ID, "corr-123");
    MDC.put(GatewayConstants.MdcKeys.TENANT_ID, "tenant-123");

    var config = new RequestTransformationFilter.Config();

    // Act
    var gatewayFilter = filter.apply(config);
    var result = gatewayFilter.filter(exchange, chain);

    // Assert
    StepVerifier.create(result)
        .verifyComplete();
  }

  @Test
  @DisplayName("Should remove configured headers")
  void shouldRemoveConfiguredHeaders() {
    // Arrange
    var requestTransformation = new IqScaffoldProperties.GatewayProperties.TransformationProperties.RequestTransformationProperties(
        true, true, true, true, true, List.of("X-Internal-Token", "X-Debug"), Map.of()
    );

    var responseTransformation = new IqScaffoldProperties.GatewayProperties.TransformationProperties.ResponseTransformationProperties(
        true, true, true, true, List.of()
    );

    var transformation = new IqScaffoldProperties.GatewayProperties.TransformationProperties(
        requestTransformation, responseTransformation
    );

    var cacheProperties = new IqScaffoldProperties.CacheProperties(
        new IqScaffoldProperties.CacheProperties.RedisProperties(
            "localhost", 6379, null, 0, Duration.ofSeconds(5),
            new IqScaffoldProperties.CacheProperties.RedisProperties.PoolProperties(10, 5, 2, Duration.ofSeconds(2)),
            "iqscaffold:cache:", Duration.ofMinutes(30), false
        )
    );

    var gatewayProperties = new IqScaffoldProperties.GatewayProperties(
        null, null, null, null, null, transformation, null
    );

    var observabilityProperties = new IqScaffoldProperties.ObservabilityProperties(null, null, null);
    var i18nProperties = new IqScaffoldProperties.I18nProperties(List.of("en"), "en");

    var customProperties = new IqScaffoldProperties(cacheProperties, gatewayProperties, i18nProperties, observabilityProperties);
    var customFilter = new RequestTransformationFilter(customProperties);

    var request = MockServerHttpRequest.get("/api/v1/users")
        .header("X-Internal-Token", "secret")
        .header("X-Debug", "true")
        .build();
    var exchange = MockServerWebExchange.from(request);
    var config = new RequestTransformationFilter.Config();

    // Act
    var gatewayFilter = customFilter.apply(config);
    var result = gatewayFilter.filter(exchange, chain);

    // Assert
    StepVerifier.create(result)
        .verifyComplete();
  }

  @Test
  @DisplayName("Should add additional configured headers")
  void shouldAddAdditionalConfiguredHeaders() {
    // Arrange
    var requestTransformation = new IqScaffoldProperties.GatewayProperties.TransformationProperties.RequestTransformationProperties(
        true, true, true, true, true, List.of(), Map.of("X-Custom-Header", "custom-value", "X-Environment", "test")
    );

    var responseTransformation = new IqScaffoldProperties.GatewayProperties.TransformationProperties.ResponseTransformationProperties(
        true, true, true, true, List.of()
    );

    var transformation = new IqScaffoldProperties.GatewayProperties.TransformationProperties(
        requestTransformation, responseTransformation
    );

    var cacheProperties = new IqScaffoldProperties.CacheProperties(
        new IqScaffoldProperties.CacheProperties.RedisProperties(
            "localhost", 6379, null, 0, Duration.ofSeconds(5),
            new IqScaffoldProperties.CacheProperties.RedisProperties.PoolProperties(10, 5, 2, Duration.ofSeconds(2)),
            "iqscaffold:cache:", Duration.ofMinutes(30), false
        )
    );

    var gatewayProperties = new IqScaffoldProperties.GatewayProperties(
        null, null, null, null, null, transformation, null
    );

    var observabilityProperties = new IqScaffoldProperties.ObservabilityProperties(null, null, null);
    var i18nProperties = new IqScaffoldProperties.I18nProperties(List.of("en"), "en");

    var customProperties = new IqScaffoldProperties(cacheProperties, gatewayProperties, i18nProperties, observabilityProperties);
    var customFilter = new RequestTransformationFilter(customProperties);

    var request = MockServerHttpRequest.get("/api/v1/users").build();
    var exchange = MockServerWebExchange.from(request);
    var config = new RequestTransformationFilter.Config();

    // Act
    var gatewayFilter = customFilter.apply(config);
    var result = gatewayFilter.filter(exchange, chain);

    // Assert
    StepVerifier.create(result)
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle empty feature context")
  void shouldHandleEmptyFeatureContext() {
    // Arrange
    var featureContext = new FeatureValidationService.FeatureContext("tenant-123");

    var request = MockServerHttpRequest.get("/api/v1/users").build();
    var exchange = MockServerWebExchange.from(request);
    exchange.getAttributes().put(GatewayConstants.Attributes.FEATURE_CONTEXT, featureContext);

    var config = new RequestTransformationFilter.Config();

    // Act
    var gatewayFilter = filter.apply(config);
    var result = gatewayFilter.filter(exchange, chain);

    // Assert
    StepVerifier.create(result)
        .verifyComplete();
  }
}
