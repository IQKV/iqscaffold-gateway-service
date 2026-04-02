package com.iqscaffold.gatewayservice.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iqscaffold.gatewayservice.exception.InvalidJwtTokenException;
import com.iqscaffold.gatewayservice.exception.MissingTenantContextException;
import com.iqscaffold.gatewayservice.exception.UnsupportedApiVersionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;
import reactor.test.StepVerifier;

@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

  private GlobalExceptionHandler exceptionHandler;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    exceptionHandler = new GlobalExceptionHandler(objectMapper);
  }

  @Test
  @DisplayName("Should handle UnsupportedApiVersionException")
  void shouldHandleUnsupportedApiVersionException() {
    var request = MockServerHttpRequest.get("/api/v5/users").build();
    var exchange = MockServerWebExchange.from(request);
    var exception = new UnsupportedApiVersionException("v5", List.of("v1", "v2"));

    var result = exceptionHandler.handle(exchange, exception);

    StepVerifier.create(result).verifyComplete();
    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  @DisplayName("Should handle MissingTenantContextException")
  void shouldHandleMissingTenantContextException() {
    var request = MockServerHttpRequest.get("/api/v1/tenants/data").build();
    var exchange = MockServerWebExchange.from(request);
    var exception = new MissingTenantContextException("/api/v1/tenants/data");

    var result = exceptionHandler.handle(exchange, exception);

    StepVerifier.create(result).verifyComplete();
    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  @DisplayName("Should handle InvalidJwtTokenException")
  void shouldHandleInvalidJwtTokenException() {
    var request = MockServerHttpRequest.get("/api/v1/users").build();
    var exchange = MockServerWebExchange.from(request);
    var exception = new InvalidJwtTokenException("Token expired",
        InvalidJwtTokenException.TokenErrorType.EXPIRED);

    var result = exceptionHandler.handle(exchange, exception);

    StepVerifier.create(result).verifyComplete();
    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  @DisplayName("Should handle generic ResponseStatusException")
  void shouldHandleGenericResponseStatusException() {
    var request = MockServerHttpRequest.get("/api/v1/users").build();
    var exchange = MockServerWebExchange.from(request);
    var exception = new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");

    var result = exceptionHandler.handle(exchange, exception);

    StepVerifier.create(result).verifyComplete();
    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  @DisplayName("Should handle SecurityException")
  void shouldHandleSecurityException() {
    var request = MockServerHttpRequest.get("/api/v1/admin/users").build();
    var exchange = MockServerWebExchange.from(request);
    var exception = new SecurityException("Access denied");

    var result = exceptionHandler.handle(exchange, exception);

    StepVerifier.create(result).verifyComplete();
    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  @DisplayName("Should handle unexpected exceptions")
  void shouldHandleUnexpectedExceptions() {
    var request = MockServerHttpRequest.get("/api/v1/users").build();
    var exchange = MockServerWebExchange.from(request);
    var exception = new RuntimeException("Unexpected error");

    var result = exceptionHandler.handle(exchange, exception);

    StepVerifier.create(result).verifyComplete();
    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
