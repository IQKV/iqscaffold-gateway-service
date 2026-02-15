package com.iqscaffold.gatewayservice.config;

import com.iqscaffold.gatewayservice.common.GatewayConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * Security configuration for the Gateway Service. Configures CORS and
 * authorization policies.
 * JWT authentication uses RSA256 validation via JWK endpoint from User Service.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfiguration {

  private final IqScaffoldProperties properties;

  public SecurityConfiguration(final IqScaffoldProperties properties) {
    this.properties = properties;
  }

  @Bean
  public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
    var publicPaths = properties.gateway().security().publicPaths();
    
    // Log public paths for debugging
    System.out.println("=== SECURITY CONFIGURATION ===");
    System.out.println("Public paths count: " + publicPaths.size());
    publicPaths.forEach(path -> System.out.println("  - " + path));
    System.out.println("==============================");
    
    return http
        .csrf(csrf -> csrf.disable())
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .authorizeExchange(exchanges -> exchanges
            // Public paths - no authentication required
            .pathMatchers(publicPaths.toArray(new String[0]))
            .permitAll()
            // Health and actuator endpoints
            .pathMatchers(HttpMethod.GET, "/actuator/health/**", "/actuator/info")
            .permitAll()
            // All other requests require authentication
            .anyExchange()
            .authenticated())
        // Use OAuth2 Resource Server with JWK Set
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> jwt.jwtDecoder(jwtDecoder())))
        .build();
  }

  @Bean
  public ReactiveJwtDecoder jwtDecoder() {
    var jwtProps = properties.gateway().security().jwt();

    if (isSymmetricConfigured()) {
      var algorithm = jwtProps.algorithm();
      javax.crypto.SecretKey key = new javax.crypto.spec.SecretKeySpec(
          jwtProps.secretKey().getBytes(), "Hmac" + algorithm.substring(2));
      return NimbusReactiveJwtDecoder.withSecretKey(key).build();
    }

    var jwkSetUri = jwtProps.jwkSetUri();
    if (jwkSetUri == null || jwkSetUri.isBlank()) {
      throw new IllegalStateException("Neither secret-key nor jwk-set-uri is configured for JWT validation");
    }
    return NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
  }

  private boolean isSymmetricConfigured() {
    var secretKey = properties.gateway().security().jwt().secretKey();
    return secretKey != null && !secretKey.isBlank() && !"change-me-in-production".equals(secretKey);
  }

  /**
   * CORS configuration source using IqScaffoldProperties settings.
   * This is the single source of CORS configuration to prevent duplicate headers.
   */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    var corsProps = properties.gateway().cors();
    var configuration = new CorsConfiguration();

    if (corsProps.enabled()) {
      // Configure allowed origins
      if (corsProps.allowedOrigins() != null && !corsProps.allowedOrigins().isEmpty()) {
        configuration.setAllowedOriginPatterns(corsProps.allowedOrigins());
      } else {
        // Default development origins
        configuration.addAllowedOriginPattern("http://localhost:*");
        configuration.addAllowedOriginPattern("https://localhost:*");
      }

      // Configure allowed methods
      if (corsProps.allowedMethods() != null && !corsProps.allowedMethods().isEmpty()) {
        configuration.setAllowedMethods(corsProps.allowedMethods());
      } else {
        // Default allowed methods
        configuration.addAllowedMethod("GET");
        configuration.addAllowedMethod("POST");
        configuration.addAllowedMethod("PUT");
        configuration.addAllowedMethod("PATCH");
        configuration.addAllowedMethod("DELETE");
        configuration.addAllowedMethod("OPTIONS");
      }

      // Configure allowed headers
      if (corsProps.allowedHeaders() != null && !corsProps.allowedHeaders().isEmpty()) {
        configuration.setAllowedHeaders(corsProps.allowedHeaders());
      } else {
        // Allow all headers by default
        configuration.addAllowedHeader("*");
      }

      // Configure credentials
      configuration.setAllowCredentials(corsProps.allowCredentials());

      // Configure max age
      configuration.setMaxAge((long) corsProps.maxAge());

      // Expose headers that clients might need
      configuration.addExposedHeader(GatewayConstants.Headers.X_CORRELATION_ID);
      configuration.addExposedHeader(GatewayConstants.Headers.X_REQUEST_ID);
      configuration.addExposedHeader(GatewayConstants.Headers.X_TOTAL_COUNT);
      configuration.addExposedHeader(GatewayConstants.Headers.X_PAGE_NUMBER);
      configuration.addExposedHeader(GatewayConstants.Headers.X_PAGE_SIZE);
    }

    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
