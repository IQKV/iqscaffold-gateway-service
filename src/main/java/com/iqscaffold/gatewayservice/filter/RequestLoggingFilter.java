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
 * This filter runs AFTER route resolution (order 10000) to see the actual target URI.
 * 
 * Spring Cloud Gateway filter order:
 * - RouteToRequestUrlFilter: 10000 (resolves target URI)
 * - NettyRoutingFilter: Integer.MAX_VALUE (makes HTTP call)
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
    
    // Get the route
    org.springframework.cloud.gateway.route.Route route = 
        exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
    
    logger.info("=== FINAL REQUEST LOGGING (Order: {}) ===", getOrder());
    logger.info("Original Request: {} {}", method, path);
    logger.info("Matched Route: {}", route != null ? route.getId() : "null");
    logger.info("Target URI: {}", targetUri);
    logger.info("Request Headers: {}", request.getHeaders().headerSet());
    logger.info("About to forward request to downstream service...");
    logger.info("==========================================");

    return chain.filter(exchange)
        .doOnSuccess(v -> {
          var response = exchange.getResponse();
          logger.info("=== RESPONSE RECEIVED ===");
          logger.info("Status Code: {}", response.getStatusCode());
          logger.info("Response Headers: {}", response.getHeaders().headerSet());
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
    // Execute AFTER RouteToRequestUrlFilter (10000) but BEFORE NettyRoutingFilter (Integer.MAX_VALUE)
    // This ensures we can see the resolved target URI
    return 10001;
  }
}
