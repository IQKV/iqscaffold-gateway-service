package com.iqscaffold.gatewayservice.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import com.iqscaffold.gatewayservice.common.GatewayConstants;
import com.iqscaffold.gatewayservice.config.IqScaffoldProperties;
import com.iqscaffold.gatewayservice.config.PlatformConfigurationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Unit tests for UnifiedMicroserviceAccessFilter.
 */
class UnifiedMicroserviceAccessFilterTest {

  private UnifiedMicroserviceAccessFilter filter;
  private GatewayFilterChain chain;
  private IqScaffoldProperties iqScaffoldProperties;
  private PlatformConfigurationProperties platformConfig;

  @BeforeEach
  void setUp() {
    iqScaffoldProperties = createTestIqScaffoldProperties();
    platformConfig = createTestPlatformConfig();
    filter = new UnifiedMicroserviceAccessFilter(iqScaffoldProperties, platformConfig);
    chain = mock(GatewayFilterChain.class);
    when(chain.filter(any())).thenReturn(Mono.empty());
  }

  private IqScaffoldProperties createTestIqScaffoldProperties() {
    // Create minimal test configuration with public paths
    var publicPaths = List.of(
        "/actuator/health",
        "/actuator/info",
        "/api/v1/auth/**",
        "/api/v1/public/**",
        "/.well-known/jwks.json"
    );

    var jwtProps = new IqScaffoldProperties.GatewayProperties.SecurityProperties.JwtProperties(
        Duration.ofMinutes(15),
        Duration.ofDays(7),
        "test-issuer",
        "test-audience",
        "HS256",
        "test-secret-key",
        "http://localhost:8080/.well-known/jwks.json"
    );

    var authProps = new IqScaffoldProperties.GatewayProperties.SecurityProperties.AuthenticationProperties(
        true,
        "http://localhost:8080",
        Duration.ofSeconds(5),
        true
    );

    var securityProps = new IqScaffoldProperties.GatewayProperties.SecurityProperties(
        jwtProps,
        authProps,
        publicPaths
    );

    // Create minimal gateway properties
    var apiPrefixProps = new IqScaffoldProperties.GatewayProperties.RoutingProperties.ApiPrefixProperties(
        true, "/api", 0
    );

    var loadBalancingProps = new IqScaffoldProperties.GatewayProperties.RoutingProperties.LoadBalancingProperties(
        "round-robin", true, Duration.ofSeconds(30)
    );

    var routingProps = new IqScaffoldProperties.GatewayProperties.RoutingProperties(
        apiPrefixProps,
        Map.of(),
        false,
        loadBalancingProps
    );

    var redisProps = new IqScaffoldProperties.GatewayProperties.RateLimitingProperties.RedisProperties(
        "test:", Duration.ofMinutes(1)
    );

    var policiesProps = new IqScaffoldProperties.GatewayProperties.RateLimitingProperties.PoliciesProperties(
        60, 100, Map.of()
    );

    var tenantQuotasProps = new IqScaffoldProperties.GatewayProperties.RateLimitingProperties.TenantQuotasProperties(
        false, 1000, Map.of()
    );

    var rateLimitingProps = new IqScaffoldProperties.GatewayProperties.RateLimitingProperties(
        true, redisProps, policiesProps, tenantQuotasProps
    );

    var circuitBreakerProps = new IqScaffoldProperties.GatewayProperties.CircuitBreakerProperties(
        true, 50, 50, Duration.ofSeconds(2), 10, Duration.ofSeconds(30), 100, "COUNT_BASED"
    );

    var corsProps = new IqScaffoldProperties.GatewayProperties.CorsProperties(
        true, List.of("*"), List.of("*"), List.of("*"), true, 3600
    );

    var requestTransformProps = new IqScaffoldProperties.GatewayProperties.TransformationProperties.RequestTransformationProperties(
        true, true, true, true, true, List.of(), Map.of()
    );

    var responseTransformProps = new IqScaffoldProperties.GatewayProperties.TransformationProperties.ResponseTransformationProperties(
        true, true, true, true, List.of()
    );

    var transformationProps = new IqScaffoldProperties.GatewayProperties.TransformationProperties(
        requestTransformProps, responseTransformProps
    );

    var featureCacheProps = new IqScaffoldProperties.GatewayProperties.FeatureAccessProperties.CacheProperties(
        Duration.ofMinutes(5), 1000, false
    );

    var featureAccessProps = new IqScaffoldProperties.GatewayProperties.FeatureAccessProperties(
        false, List.of(), List.of(), featureCacheProps
    );

    var gatewayProps = new IqScaffoldProperties.GatewayProperties(
        routingProps,
        securityProps,
        rateLimitingProps,
        circuitBreakerProps,
        corsProps,
        transformationProps,
        featureAccessProps
    );

    // Create minimal cache properties
    var cachePoolProps = new IqScaffoldProperties.CacheProperties.RedisProperties.PoolProperties(
        8, 8, 0, Duration.ofMillis(-1)
    );

    var cacheRedisProps = new IqScaffoldProperties.CacheProperties.RedisProperties(
        "localhost", 6379, "", 0, Duration.ofSeconds(2), cachePoolProps, "test:", Duration.ofHours(1), false
    );

    var cacheProps = new IqScaffoldProperties.CacheProperties(cacheRedisProps);

    // Create minimal i18n properties
    var i18nProps = new IqScaffoldProperties.I18nProperties(List.of("en"), "en");

    // Create minimal observability properties
    var tracingProps = new IqScaffoldProperties.ObservabilityProperties.TracingProperties(
        true, "test-service", 1.0, "http://localhost:4317", Duration.ofSeconds(10), Duration.ofSeconds(30), 512
    );

    var metricsProps = new IqScaffoldProperties.ObservabilityProperties.MetricsProperties(
        true, "/actuator/prometheus", "test", true, true, true, Map.of(), List.of()
    );

    var loggingProps = new IqScaffoldProperties.ObservabilityProperties.LoggingProperties(
        "INFO", "console", true, false, true, true, true, true, true,
        "X-Correlation-ID", "X-Request-ID", "X-Tenant-ID"
    );

    var observabilityProps = new IqScaffoldProperties.ObservabilityProperties(
        tracingProps, metricsProps, loggingProps
    );

    return new IqScaffoldProperties(cacheProps, gatewayProps, i18nProps, observabilityProps);
  }

  private PlatformConfigurationProperties createTestPlatformConfig() {
    var routeProtection = new PlatformConfigurationProperties.Security.RouteProtection(
        List.of("/actuator/**", "/api/*/auth/**", "/api/*/health"),
        Map.of(
            "/api/*/crm/**", List.of(GatewayConstants.Authorities.CRM_ACCESS, GatewayConstants.Authorities.ADMIN, GatewayConstants.Authorities.SUPER_ADMIN),
            "/api/*/billing/**", List.of(GatewayConstants.Authorities.BILLING_ACCESS, GatewayConstants.Authorities.ADMIN, GatewayConstants.Authorities.SUPER_ADMIN),
            "/api/*/admin/**", List.of(GatewayConstants.Authorities.ADMIN, GatewayConstants.Authorities.SUPER_ADMIN)
        )
    );

    var accessControl = new PlatformConfigurationProperties.Security.AccessControl(
        true, true, true,
        List.of(GatewayConstants.Authorities.SUPER_ADMIN, GatewayConstants.Authorities.ADMIN)
    );

    var security = new PlatformConfigurationProperties.Security(routeProtection, accessControl);

    return new PlatformConfigurationProperties(security);
  }

  @Test
  @DisplayName("Should allow access to public paths without authentication")
  void shouldAllowAccessToPublicPaths() {
    // Arrange - test with auth signup endpoint
    var request = MockServerHttpRequest.post("/api/v1/auth/signup").build();
    var exchange = MockServerWebExchange.from(request);

    // Act
    var result = filter.filter(exchange, chain);

    // Assert
    StepVerifier.create(result)
        .verifyComplete();
  }

  @Test
  @DisplayName("Should allow access to actuator health endpoint")
  void shouldAllowAccessToActuatorHealth() {
    // Arrange
    var request = MockServerHttpRequest.get("/actuator/health").build();
    var exchange = MockServerWebExchange.from(request);

    // Act
    var result = filter.filter(exchange, chain);

    // Assert
    StepVerifier.create(result)
        .verifyComplete();
  }

  @Test
  @DisplayName("Should allow access when user has required CRM authority")
  void shouldAllowAccessWithCrmAuthority() {
    // Arrange
    var request = MockServerHttpRequest.get("/api/v1/crm/contacts")
        .header(GatewayConstants.Headers.X_USER_AUTHORITIES, GatewayConstants.Authorities.CRM_ACCESS)
        .header(GatewayConstants.Headers.X_USERNAME, "john.doe")
        .build();
    var exchange = MockServerWebExchange.from(request);

    // Act
    var result = filter.filter(exchange, chain);

    // Assert
    StepVerifier.create(result)
        .verifyComplete();
  }

  @Test
  @DisplayName("Should allow access when user has ADMIN authority")
  void shouldAllowAccessWithAdminAuthority() {
    // Arrange
    var request = MockServerHttpRequest.get("/api/v1/crm/leads")
        .header(GatewayConstants.Headers.X_USER_AUTHORITIES, GatewayConstants.Authorities.ADMIN)
        .header(GatewayConstants.Headers.X_USERNAME, "admin.user")
        .build();
    var exchange = MockServerWebExchange.from(request);

    // Act
    var result = filter.filter(exchange, chain);

    // Assert
    StepVerifier.create(result)
        .verifyComplete();
  }

  @Test
  @DisplayName("Should allow access when user has SUPER_ADMIN authority")
  void shouldAllowAccessWithSuperAdminAuthority() {
    // Arrange
    var request = MockServerHttpRequest.get("/api/v1/billing/invoices")
        .header(GatewayConstants.Headers.X_USER_AUTHORITIES, GatewayConstants.Authorities.SUPER_ADMIN)
        .header(GatewayConstants.Headers.X_USERNAME, "super.admin")
        .build();
    var exchange = MockServerWebExchange.from(request);

    // Act
    var result = filter.filter(exchange, chain);

    // Assert
    StepVerifier.create(result)
        .verifyComplete();
  }

  @Test
  @DisplayName("Should deny access when user lacks required authority")
  void shouldDenyAccessWhenLackingAuthority() {
    // Arrange
    var request = MockServerHttpRequest.get("/api/v1/crm/contacts")
        .header(GatewayConstants.Headers.X_USER_AUTHORITIES, GatewayConstants.Authorities.USER)
        .header(GatewayConstants.Headers.X_USERNAME, "regular.user")
        .build();
    var exchange = MockServerWebExchange.from(request);

    // Act
    var result = filter.filter(exchange, chain);

    // Assert
    StepVerifier.create(result)
        .expectComplete()
        .verify();

    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  @DisplayName("Should deny access when no authorities header present")
  void shouldDenyAccessWhenNoAuthoritiesHeader() {
    // Arrange
    var request = MockServerHttpRequest.get("/api/v1/billing/payments")
        .header(GatewayConstants.Headers.X_USERNAME, "user.without.auth")
        .build();
    var exchange = MockServerWebExchange.from(request);

    // Act
    var result = filter.filter(exchange, chain);

    // Assert
    StepVerifier.create(result)
        .expectComplete()
        .verify();

    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  @DisplayName("Should allow access to unprotected routes")
  void shouldAllowAccessToUnprotectedRoutes() {
    // Arrange
    var request = MockServerHttpRequest.get("/api/v1/users/profile")
        .header(GatewayConstants.Headers.X_USER_AUTHORITIES, GatewayConstants.Authorities.USER)
        .build();
    var exchange = MockServerWebExchange.from(request);

    // Act
    var result = filter.filter(exchange, chain);

    // Assert
    StepVerifier.create(result)
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle multiple authorities correctly")
  void shouldHandleMultipleAuthorities() {
    // Arrange
    var request = MockServerHttpRequest.get("/api/v1/crm/pipeline")
        .header(GatewayConstants.Headers.X_USER_AUTHORITIES, "USER,CRM_ACCESS,BILLING_ACCESS")
        .header(GatewayConstants.Headers.X_USERNAME, "multi.auth.user")
        .build();
    var exchange = MockServerWebExchange.from(request);

    // Act
    var result = filter.filter(exchange, chain);

    // Assert
    StepVerifier.create(result)
        .verifyComplete();
  }

  @Test
  @DisplayName("Should deny access to admin routes for regular users")
  void shouldDenyAccessToAdminRoutes() {
    // Arrange
    var request = MockServerHttpRequest.get("/api/v1/admin/users")
        .header(GatewayConstants.Headers.X_USER_AUTHORITIES, GatewayConstants.Authorities.USER)
        .header(GatewayConstants.Headers.X_USERNAME, "regular.user")
        .build();
    var exchange = MockServerWebExchange.from(request);

    // Act
    var result = filter.filter(exchange, chain);

    // Assert
    StepVerifier.create(result)
        .expectComplete()
        .verify();

    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  @DisplayName("Should allow access to admin routes for admin users")
  void shouldAllowAccessToAdminRoutesForAdmins() {
    // Arrange
    var request = MockServerHttpRequest.get("/api/v1/admin/settings")
        .header(GatewayConstants.Headers.X_USER_AUTHORITIES, GatewayConstants.Authorities.ADMIN)
        .header(GatewayConstants.Headers.X_USERNAME, "admin.user")
        .build();
    var exchange = MockServerWebExchange.from(request);

    // Act
    var result = filter.filter(exchange, chain);

    // Assert
    StepVerifier.create(result)
        .verifyComplete();
  }

  @Test
  @DisplayName("Should return correct order")
  void shouldReturnCorrectOrder() {
    // Act
    var order = filter.getOrder();

    // Assert
    assertThat(order).isEqualTo(GatewayConstants.FilterOrder.JWT_AUTHENTICATION_FILTER + 10);
  }

  @Test
  @DisplayName("Should handle auth endpoint as public")
  void shouldHandleAuthEndpointAsPublic() {
    // Arrange
    var request = MockServerHttpRequest.post("/api/v1/auth/login").build();
    var exchange = MockServerWebExchange.from(request);

    // Act
    var result = filter.filter(exchange, chain);

    // Assert
    StepVerifier.create(result)
        .verifyComplete();
  }

  @Test
  @DisplayName("Should deny access with empty authorities header")
  void shouldDenyAccessWithEmptyAuthoritiesHeader() {
    // Arrange
    var request = MockServerHttpRequest.get("/api/v1/billing/subscriptions")
        .header(GatewayConstants.Headers.X_USER_AUTHORITIES, "")
        .build();
    var exchange = MockServerWebExchange.from(request);

    // Act
    var result = filter.filter(exchange, chain);

    // Assert
    StepVerifier.create(result)
        .expectComplete()
        .verify();

    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  @DisplayName("Should handle billing routes correctly")
  void shouldHandleBillingRoutes() {
    // Arrange
    var request = MockServerHttpRequest.get("/api/v1/billing/invoices")
        .header(GatewayConstants.Headers.X_USER_AUTHORITIES, GatewayConstants.Authorities.BILLING_ACCESS)
        .header(GatewayConstants.Headers.X_USERNAME, "billing.user")
        .build();
    var exchange = MockServerWebExchange.from(request);

    // Act
    var result = filter.filter(exchange, chain);

    // Assert
    StepVerifier.create(result)
        .verifyComplete();
  }
}
