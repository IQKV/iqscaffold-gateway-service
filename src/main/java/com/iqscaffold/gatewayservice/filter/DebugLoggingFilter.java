package com.iqscaffold.gatewayservice.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Debug filter to trace request flow through the filter chain.
 */
@Component
public class DebugLoggingFilter implements GlobalFilter, Ordered {

  private static final Logger logger = LoggerFactory.getLogger(DebugLoggingFilter.class);

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    var path = exchange.getRequest().getPath().value();
    
    logger.info(">>> DEBUG FILTER (Order: {}) - BEFORE chain.filter() for path: {}", getOrder(), path);
    
    return chain.filter(exchange)
        .doOnSuccess(v -> logger.info(">>> DEBUG FILTER - AFTER chain.filter() SUCCESS for path: {}", path))
        .doOnError(e -> logger.error(">>> DEBUG FILTER - AFTER chain.filter() ERROR for path: {}", path, e))
        .doFinally(signal -> logger.info(">>> DEBUG FILTER - FINALLY signal: {} for path: {}", signal, path));
  }

  @Override
  public int getOrder() {
    // Run at multiple points to trace execution
    return -10; // After circuit breaker (-25) but before route resolution (10000)
  }
}
