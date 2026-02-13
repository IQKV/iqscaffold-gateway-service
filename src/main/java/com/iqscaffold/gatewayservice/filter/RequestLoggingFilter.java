package com.iqscaffold.gatewayservice.filter;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Filter to log the final request details right before it's sent to the downstream service.
 * This filter runs with a very low order (high priority number) to execute just before
 * the NettyRoutingFilter sends the actual HTTP request.
 */
@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

  private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    var request = exchange.getRequest();
    var path = request.getPath().value();
    var method = request.getMethod();
    
    // Get the target URI that Spring Cloud Gateway has resolved
    URI targetUri = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
    
    logger.info("=== FINAL REQUEST LOGGING ===");
    logger.info("Original Request: {} {}", method, path);
    logger.info("Target URI: {}", targetUri);
    logger.info("Request Headers: {}", request.getHeaders().keySet());
    logger.info("About to forward request to downstream service...");
    logger.info("=============================");

    return chain.filter(exchange)
        .doOnSuccess(v -> {
          var response = exchange.getResponse();
          logger.info("=== RESPONSE RECEIVED ===");
          logger.info("Status Code: {}", response.getStatusCode());
          logger.info("Response Headers: {}", response.getHeaders().keySet());
          logger.info("=========================");
        })
        .doOnError(e -> {
          logger.error("=== REQUEST FAILED ===");
          logger.error("Error Type: {}", e.getClass().getSimpleName());
          logger.error("Error Message: {}", e.getMessage());
          logger.error("======================", e);
        });
  }

  @Override
  public int getOrder() {
    // Execute just before NettyRoutingFilter (which has order Integer.MAX_VALUE)
    // NettyRoutingFilter order is typically around 2147483647
    // We want to run right before it, so use a high number
    return Ordered.LOWEST_PRECEDENCE - 1;
  }
}
