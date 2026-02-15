package com.iqscaffold.gatewayservice.filter;

import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Global filter that intercepts error responses from downstream services
 * and ensures Problem Details are properly formatted without Java type information.
 */
@Component
public class DownstreamErrorResponseFilter implements GlobalFilter, Ordered {

  private static final Logger logger = LoggerFactory.getLogger(DownstreamErrorResponseFilter.class);
  private final ObjectMapper objectMapper;

  public DownstreamErrorResponseFilter(final ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    ServerHttpResponse originalResponse = exchange.getResponse();
    
    ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {
      @Override
      public Mono<Void> writeWith(org.reactivestreams.Publisher<? extends DataBuffer> body) {
        var statusCodeValue = getStatusCode();
        if (statusCodeValue == null) {
          return super.writeWith(body);
        }
        
        HttpStatus statusCode = HttpStatus.valueOf(statusCodeValue.value());
        var contentType = getHeaders().getContentType();
        
        // Only intercept error responses with application/problem+json content type
        if (statusCode.isError()
            && contentType != null
            && contentType.isCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)) {
          
          if (body instanceof Flux) {
            Flux<? extends DataBuffer> fluxBody = (Flux<? extends DataBuffer>) body;
            
            return fluxBody
                .collectList()
                .flatMap(dataBuffers -> {
                  DataBuffer joinedBuffer = originalResponse.bufferFactory().join(dataBuffers);
                  byte[] content = new byte[joinedBuffer.readableByteCount()];
                  joinedBuffer.read(content);
                  DataBufferUtils.release(joinedBuffer);
                  
                  String responseBody = new String(content, StandardCharsets.UTF_8);
                  
                  try {
                    // Try to parse as ProblemDetail
                    ProblemDetail problemDetail = objectMapper.readValue(responseBody, ProblemDetail.class);
                    
                    // Re-serialize without type information
                    String cleanJson = objectMapper.writeValueAsString(problemDetail);
                    
                    logger.debug("Cleaned downstream error response for status {}", statusCode);
                    
                    byte[] cleanBytes = cleanJson.getBytes(StandardCharsets.UTF_8);
                    DataBuffer buffer = originalResponse.bufferFactory().wrap(cleanBytes);
                    
                    // Update content length
                    getHeaders().setContentLength(cleanBytes.length);
                    
                    return super.writeWith(Mono.just(buffer));
                  } catch (final Exception e) {
                    // If parsing fails, pass through original response
                    logger.warn("Failed to parse downstream error response as ProblemDetail: {}", e.getMessage());
                    DataBuffer buffer = originalResponse.bufferFactory().wrap(content);
                    return super.writeWith(Mono.just(buffer));
                  }
                });
          }
        }
        
        // For non-error responses or non-problem+json, pass through
        return super.writeWith(body);
      }
    };
    
    return chain.filter(exchange.mutate().response(decoratedResponse).build());
  }

  @Override
  public int getOrder() {
    // Run after routing but before response transformation
    return -1;
  }
}
