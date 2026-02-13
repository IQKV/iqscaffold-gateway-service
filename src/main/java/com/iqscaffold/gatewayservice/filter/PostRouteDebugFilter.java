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
 * Debug filter that runs AFTER route resolution to verify target URI.
 */
@Component
public class PostRouteDebugFilter implements GlobalFilter, Ordered {

  private static final Logger logger = LoggerFactory.getLogger(PostRouteDebugFilter.class);

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    var path = exchange.getRequest().getPath().value();
    URI targetUri = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
    var route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
    
    logger.info(">>> POST-ROUTE DEBUG (Order: {}) - path: {}, route: {}, targetUri: {}", 
        getOrder(), path, route, targetUri);
    
    return chain.filter(exchange)
        .doOnSuccess(v -> {
          var response = exchange.getResponse();
          logger.info(">>> POST-ROUTE DEBUG - Response status: {} for path: {}", 
              response.getStatusCode(), path);
        })
        .doOnError(e -> logger.error(">>> POST-ROUTE DEBUG - Error for path: {}", path, e));
  }

  @Override
  public int getOrder() {
    // Run AFTER RouteToRequestUrlFilter (10000)
    return 10500;
  }
}
