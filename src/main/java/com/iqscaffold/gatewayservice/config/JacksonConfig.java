package com.iqscaffold.gatewayservice.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Jackson configuration for the Gateway Service.
 * Ensures proper JSON serialization without type information.
 */
@Configuration
public class JacksonConfig {

  @Bean
  @Primary
  public ObjectMapper objectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    
    // Register Java 8 date/time module
    mapper.registerModule(new JavaTimeModule());
    
    // Disable writing dates as timestamps
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    
    // Don't fail on unknown properties
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    
    // Don't include null values
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    
    // CRITICAL: Disable default typing to prevent type information in JSON
    // This ensures ProblemDetail objects are serialized without Java type metadata
    mapper.deactivateDefaultTyping();
    
    return mapper;
  }
}
