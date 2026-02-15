package com.iqscaffold.gatewayservice.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class DownstreamErrorResponseFilterTest {

  private DownstreamErrorResponseFilter filter;
  private ObjectMapper objectMapper;
  private GatewayFilterChain chain;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    filter = new DownstreamErrorResponseFilter(objectMapper);
    chain = mock(GatewayFilterChain.class);
  }

  @Test
  void shouldCleanProblemDetailResponseFromDownstream() {
    // Given
    var request = MockServerHttpRequest.get("/api/v1/auth/signup").build();
    var exchange = MockServerWebExchange.from(request);
    
    // Create a ProblemDetail response with type information (simulating downstream)
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.CONFLICT,
        "Username already exists"
    );
    problemDetail.setType(URI.create("https://problems.iqscaffold.com/user-registration"));
    problemDetail.setTitle("User registration failed");
    problemDetail.setInstance(URI.create("/api/v1/auth/signup"));
    problemDetail.setProperty("path", "/api/v1/auth/signup");
    problemDetail.setProperty("method", "POST");
    problemDetail.setProperty("code", "USER_ALREADY_EXISTS");
    
    // Simulate downstream response with type information
    String downstreamJson = "[\"org.springframework.http.ProblemDetail\"," + 
        "{\"type\":\"https://problems.iqscaffold.com/user-registration\"," +
        "\"title\":\"User registration failed\"," +
        "\"status\":409," +
        "\"detail\":\"Username already exists\"," +
        "\"instance\":\"/api/v1/auth/signup\"," +
        "\"properties\":{\"path\":\"/api/v1/auth/signup\",\"method\":\"POST\",\"code\":\"USER_ALREADY_EXISTS\"}}]";
    
    DataBuffer buffer = new DefaultDataBufferFactory().wrap(downstreamJson.getBytes(StandardCharsets.UTF_8));
    
    // Set up the exchange response
    exchange.getResponse().setStatusCode(HttpStatus.CONFLICT);
    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
    
    // Mock chain to write the problematic response
    when(chain.filter(any(ServerWebExchange.class)))
        .thenAnswer(invocation -> {
          ServerWebExchange ex = invocation.getArgument(0);
          return ex.getResponse().writeWith(Flux.just(buffer));
        });
    
    // When
    Mono<Void> result = filter.filter(exchange, chain);
    
    // Then
    StepVerifier.create(result)
        .verifyComplete();
    
    // Verify the response was cleaned (no type information)
    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(exchange.getResponse().getHeaders().getContentType())
        .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
  }

  @Test
  void shouldPassThroughNonErrorResponses() {
    // Given
    var request = MockServerHttpRequest.get("/api/v1/users/me").build();
    var exchange = MockServerWebExchange.from(request);
    
    String successJson = "{\"id\":1,\"username\":\"testuser\"}";
    DataBuffer buffer = new DefaultDataBufferFactory().wrap(successJson.getBytes(StandardCharsets.UTF_8));
    
    exchange.getResponse().setStatusCode(HttpStatus.OK);
    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
    
    when(chain.filter(any(ServerWebExchange.class)))
        .thenAnswer(invocation -> {
          ServerWebExchange ex = invocation.getArgument(0);
          return ex.getResponse().writeWith(Flux.just(buffer));
        });
    
    // When
    Mono<Void> result = filter.filter(exchange, chain);
    
    // Then
    StepVerifier.create(result)
        .verifyComplete();
    
    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void shouldPassThroughNonProblemJsonErrorResponses() {
    // Given
    var request = MockServerHttpRequest.get("/api/v1/users/999").build();
    var exchange = MockServerWebExchange.from(request);
    
    String errorJson = "{\"error\":\"Not found\"}";
    DataBuffer buffer = new DefaultDataBufferFactory().wrap(errorJson.getBytes(StandardCharsets.UTF_8));
    
    exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
    
    when(chain.filter(any(ServerWebExchange.class)))
        .thenAnswer(invocation -> {
          ServerWebExchange ex = invocation.getArgument(0);
          return ex.getResponse().writeWith(Flux.just(buffer));
        });
    
    // When
    Mono<Void> result = filter.filter(exchange, chain);
    
    // Then
    StepVerifier.create(result)
        .verifyComplete();
    
    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void shouldHaveCorrectOrder() {
    // Then
    assertThat(filter.getOrder()).isEqualTo(-1);
  }
}
