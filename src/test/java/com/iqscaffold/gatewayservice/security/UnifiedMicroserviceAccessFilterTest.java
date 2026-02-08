package com.iqscaffold.gatewayservice.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import com.iqscaffold.gatewayservice.common.GatewayConstants;
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
  private PlatformConfigurationProperties platformConfig;

  @BeforeEach
  void setUp() {
    platformConfig = createTestPlatformConfig();
    filter = new UnifiedMicroserviceAccessFilter(platformConfig);
    chain = mock(GatewayFilterChain.class);
    when(chain.filter(any())).thenReturn(Mono.empty());
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
