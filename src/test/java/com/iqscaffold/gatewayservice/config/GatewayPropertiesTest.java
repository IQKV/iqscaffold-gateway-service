package com.iqscaffold.gatewayservice.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("GatewayProperties Tests")
class GatewayPropertiesTest {

  private Validator validator;

  @BeforeEach
  void setUp() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @Nested
  @DisplayName("ApiPrefixProperties Tests")
  class ApiPrefixPropertiesTests {

    @Test
    @DisplayName("Should validate successfully with valid API prefix properties")
    void shouldValidateSuccessfully() {
      var props = new IqScaffoldProperties.GatewayProperties.RoutingProperties.ApiPrefixProperties(true, "/api", 0);
      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.RoutingProperties.ApiPrefixProperties>> violations = validator.validate(props);
      assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail validation when prefix is blank")
    void shouldFailWhenPrefixIsBlank() {
      var props = new IqScaffoldProperties.GatewayProperties.RoutingProperties.ApiPrefixProperties(true, "", 0);
      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.RoutingProperties.ApiPrefixProperties>> violations = validator.validate(props);
      assertThat(violations).isNotEmpty();
      assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("prefix"));
    }

    @Test
    @DisplayName("Should fail validation when stripCount is negative")
    void shouldFailWhenStripCountIsNegative() {
      var props = new IqScaffoldProperties.GatewayProperties.RoutingProperties.ApiPrefixProperties(true, "/api", -1);
      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.RoutingProperties.ApiPrefixProperties>> violations = validator.validate(props);
      assertThat(violations).isNotEmpty();
    }
  }

  @Nested
  @DisplayName("ServiceProperties Tests")
  class ServicePropertiesTests {

    @Test
    @DisplayName("Should validate successfully with valid service properties")
    void shouldValidateSuccessfully() {
      var props = new IqScaffoldProperties.GatewayProperties.RoutingProperties.ServiceProperties(
          "http://service:8080", "/v1/**", true, 5000, 30000, null);
      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.RoutingProperties.ServiceProperties>> violations = validator.validate(props);
      assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail validation when uri is blank")
    void shouldFailWhenUriIsBlank() {
      var props = new IqScaffoldProperties.GatewayProperties.RoutingProperties.ServiceProperties(
          "", "/v1/**", true, 5000, 30000, null);
      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.RoutingProperties.ServiceProperties>> violations = validator.validate(props);
      assertThat(violations).isNotEmpty();
      assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("uri"));
    }

    @Test
    @DisplayName("Should fail validation when connectTimeout is not positive")
    void shouldFailWhenConnectTimeoutIsZero() {
      var props = new IqScaffoldProperties.GatewayProperties.RoutingProperties.ServiceProperties(
          "http://service:8080", "/v1/**", true, 0, 30000, null);
      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.RoutingProperties.ServiceProperties>> violations = validator.validate(props);
      assertThat(violations).isNotEmpty();
    }
  }

  @Nested
  @DisplayName("OpenApiProperties Tests")
  class OpenApiPropertiesTests {

    @Test
    @DisplayName("Should apply default values when fields are null")
    void shouldApplyDefaultValuesWhenNull() {
      var props = new IqScaffoldProperties.GatewayProperties.RoutingProperties.ServiceProperties.OpenApiProperties(
          true, null, null, null);
      assertThat(props.displayName()).isEqualTo("Service API");
      assertThat(props.description()).isEqualTo("API documentation");
      assertThat(props.contextPath()).isEqualTo("");
    }

    @Test
    @DisplayName("Should preserve custom values when provided")
    void shouldPreserveCustomValues() {
      var props = new IqScaffoldProperties.GatewayProperties.RoutingProperties.ServiceProperties.OpenApiProperties(
          true, "User Service", "User management API", "/api/users");
      assertThat(props.displayName()).isEqualTo("User Service");
      assertThat(props.description()).isEqualTo("User management API");
      assertThat(props.contextPath()).isEqualTo("/api/users");
    }
  }

  @Nested
  @DisplayName("JwtProperties Tests")
  class JwtPropertiesTests {

    @Test
    @DisplayName("Should validate successfully with HS256 algorithm")
    void shouldValidateWithHS256() {
      var props = new IqScaffoldProperties.GatewayProperties.SecurityProperties.JwtProperties(
          Duration.ofMinutes(15), Duration.ofDays(7), "issuer", "audience", "HS256",
          "secret-key", "http://none");
      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.SecurityProperties.JwtProperties>> violations = validator.validate(props);
      assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail validation with invalid algorithm")
    void shouldFailWithInvalidAlgorithm() {
      var props = new IqScaffoldProperties.GatewayProperties.SecurityProperties.JwtProperties(
          Duration.ofMinutes(15), Duration.ofDays(7), "issuer", "audience", "ES256",
          "none", "http://auth/.well-known/jwks.json");
      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.SecurityProperties.JwtProperties>> violations = validator.validate(props);
      assertThat(violations).isNotEmpty();
      assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("algorithm"));
    }

    @Test
    @DisplayName("Should fail validation when issuer is blank")
    void shouldFailWhenIssuerIsBlank() {
      var props = new IqScaffoldProperties.GatewayProperties.SecurityProperties.JwtProperties(
          Duration.ofMinutes(15), Duration.ofDays(7), "", "audience", "RS256",
          "none", "http://auth/.well-known/jwks.json");
      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.SecurityProperties.JwtProperties>> violations = validator.validate(props);
      assertThat(violations).isNotEmpty();
    }
  }

  @Nested
  @DisplayName("AuthenticationProperties Tests")
  class AuthenticationPropertiesTests {

    @Test
    @DisplayName("Should validate successfully with valid authentication properties")
    void shouldValidateSuccessfully() {
      var props = new IqScaffoldProperties.GatewayProperties.SecurityProperties.AuthenticationProperties(
          true, "http://user-service:8080", Duration.ofSeconds(5), true);
      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.SecurityProperties.AuthenticationProperties>> violations = validator.validate(props);
      assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail validation when userServiceUrl is blank")
    void shouldFailWhenUserServiceUrlIsBlank() {
      var props = new IqScaffoldProperties.GatewayProperties.SecurityProperties.AuthenticationProperties(
          true, "", Duration.ofSeconds(5), true);
      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.SecurityProperties.AuthenticationProperties>> violations = validator.validate(props);
      assertThat(violations).isNotEmpty();
    }
  }

  @Nested
  @DisplayName("CorsProperties Tests")
  class CorsPropertiesTests {

    @Test
    @DisplayName("Should validate successfully with valid CORS properties")
    void shouldValidateSuccessfully() {
      var props = new IqScaffoldProperties.GatewayProperties.CorsProperties(
          true, List.of("http://localhost:3000"), List.of("GET", "POST"),
          List.of("*"), true, 3600);
      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.CorsProperties>> violations = validator.validate(props);
      assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail validation when maxAge exceeds maximum")
    void shouldFailWhenMaxAgeExceedsMaximum() {
      var props = new IqScaffoldProperties.GatewayProperties.CorsProperties(
          true, List.of("http://localhost:3000"), List.of("GET"),
          List.of("*"), true, 90000);
      Set<ConstraintViolation<IqScaffoldProperties.GatewayProperties.CorsProperties>> violations = validator.validate(props);
      assertThat(violations).isNotEmpty();
    }
  }
}
