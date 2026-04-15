# 🌐 IQ Key Value Gateway Service

> Reactive API gateway providing intelligent routing, JWT authentication, tenant context propagation, feature-based access control, and observability across microservices.

## Table of Contents

- [Business Purpose](#business-purpose)
- [Overview](#overview)
- [What It Demonstrates](#what-it-demonstrates)
- [Architecture Patterns](#architecture-patterns)
- [Technical Highlights](#technical-highlights)
- [Use Cases Implemented](#use-cases-implemented)
- [API Examples](#api-examples)
- [Learning Points](#learning-points)
- [Adapting for Your Domain](#adapting-for-your-domain)
- [Integration with Downstream Services](#integration-with-downstream-services)
- [Deployment Guide](docs/deployment/README.md)

## Business Purpose

A centralized entry point for the IQ Key Value microservices platform that handles:

- **Intelligent Routing** - Dynamic request routing to downstream services with path-based and header-based versioning
- **Authentication Gateway** - JWT validation and user context propagation to all protected services
- **Feature Access Control** - Validates tenant subscription features before forwarding requests
- **Multi-Tenancy** - Tenant context extraction from headers or JWT claims and propagation downstream
- **Request Transformation** - Header enrichment, correlation ID generation, and context propagation
- **Observability** - Distributed tracing, Prometheus metrics, and structured logging

## Overview

This is the front door to the IQ Key Value microservices ecosystem. Built on Spring Cloud Gateway with reactive programming, it provides a single entry point for all client requests while handling cross-cutting concerns like authentication, authorization, feature validation, and observability.

## What It Demonstrates

### 🌐 Reactive Gateway Patterns

- Spring Cloud Gateway with WebFlux for non-blocking I/O
- Reactive filter chains with ordered execution (`GlobalFilter` + `Ordered`)
- Reactive JWT validation with OAuth2 Resource Server
- Reactive Redis operations with `ReactiveStringRedisTemplate`

### 🔐 Authentication & Authorization

- JWT validation using RSA256 with JWK Set endpoint
- User context extraction (userId, username, email, authorities, permissions, organizationId)
- Authority propagation via headers (X-User-Authorities, X-User-Email, X-User-Permissions, X-Organization-ID)
- Header sanitization to prevent spoofing attacks (removes all user/tenant context headers from incoming requests)
- Public path pattern matching (exact and wildcard `/**`)
- Route-level authority enforcement via `PlatformConfigurationProperties`
- MDC logging with user and tenant context

### 🎟️ Feature Access Control

- Validates tenant subscription features before routing requests
- Communicates with billing service to retrieve tenant feature context
- Supports boolean features, quota-based features, limit-based features, and tier-based features
- Fail-open policy on validation service errors (requests proceed if billing service is unavailable)
- Feature context propagated to downstream services via exchange attributes

### 🏢 Multi-Tenancy Support

- Priority-based tenant extraction (JWT claims → X-Tenant-ID header)
- Tenant context stored in exchange attributes and propagated downstream
- Tenant ID forwarded via X-Tenant-ID header to all services

### 🎯 Observability & Monitoring

- Correlation ID generation and propagation across all requests
- OpenTelemetry distributed tracing with OTLP export
- Prometheus metrics for gateway operations
- Structured JSON logging with MDC context (user ID, tenant ID, correlation ID, trace ID)
- Custom health indicators for Redis and downstream user service

## Architecture Patterns

### Reactive Filter Chain

```
Request Flow:
1. CorrelationIdFilter          → Generate/extract correlation ID, set MDC context
2. TenantExtractionFilter       → Extract tenant context from JWT or header
3. JwtAuthenticationFilter      → Validate JWT and extract user context
4. UnifiedMicroserviceAccessFilter → Enforce route-level authority requirements
5. ApiVersionRoutingFilter      → Handle API versioning
6. FeatureAccessFilter          → Validate tenant feature access
7. RequestTransformationFilter  → Enrich headers with user/tenant context
8. Route to downstream service
9. ResponseTransformationFilter → Add security headers, remove internal headers
```

### API Design

- Centralized routing configuration in YAML and profile-based Java config
- Path-based and header-based API versioning
- Public vs protected endpoint segregation
- OpenAPI documentation aggregation from all downstream services
- Consistent error responses with Problem Details (RFC 7807)

## Technical Highlights

### Reactive Programming

- Non-blocking I/O with Project Reactor (Mono/Flux)
- Reactive Redis operations with `ReactiveStringRedisTemplate`
- `ReactiveSecurityContextHolder` for JWT validation
- Reactive filter chains with `flatMap` and `transformDeferred`

### Security Features

- JWT validation with RSA256 public key via JWK Set URI
- CORS configuration per environment
- Security header injection on all responses
- Internal header removal from responses
- Route protection with authority-based access control

### Configuration Management

- Type-safe configuration with Java records (`IqScaffoldProperties`)
- Nested record structure for organized config hierarchy
- Bean Validation annotations (`@Min`, `@Max`, `@NotBlank`, `@Pattern`)
- Environment-specific profiles (local, staging, production)
- Profile-based route configuration (crm, billing profiles)

### Operational Features

- Docker containerization
- Health checks and actuator endpoints
- Structured JSON logging
- Prometheus metrics export
- OpenAPI documentation aggregation via SpringDoc

## Use Cases Implemented

### Request Routing

- Route requests to user-service (`/api/v1/auth/**`, `/api/v1/users/me`, `/api/v1/admin/**`)
- Route requests to billing-service (`/api/v1/billing/**`, `/api/v1/features/**`, `/api/v1/internal/features/**`)
- Route requests to CRM services (`/api/v1/leads/**`, `/api/v1/contacts/**`, `/api/v1/pipeline/**`, `/api/v1/companies/**`)
- Profile-based route activation (crm, billing profiles)

### Authentication Flow

- Validate JWT tokens from Authorization header
- Extract user context (userId, username, email, authorities, permissions, organizationId)
- Sanitize incoming headers to prevent spoofing (removes X-User-\*, X-Tenant-ID, X-Organization-ID)
- Propagate user context to downstream services via headers:
    - `X-User-ID` - User identifier
    - `X-Username` - Username
    - `X-User-Email` - User email address
    - `X-User-Authorities` - Comma-separated list of authorities (e.g., `ADMIN,USER`)
    - `X-User-Permissions` - Comma-separated list of permissions
    - `X-Tenant-ID` - Tenant identifier
    - `X-Organization-ID` - Organization identifier
- Skip authentication for public paths

### Feature Access Control

- Map endpoints to required subscription features via configuration
- Validate tenant has access to required features before routing
- Block requests to disabled features with `403 Forbidden`
- Track feature usage asynchronously for billing analytics

### Multi-Tenancy

- Extract tenant from X-Tenant-ID header or JWT claims
- Tenant context propagation to all downstream services

### Request Transformation

- Add correlation ID to all requests
- Sanitize incoming headers (remove X-User-\*, X-Tenant-ID, X-Organization-ID to prevent spoofing)
- Propagate user and tenant context headers
- Add gateway version header
- Remove internal headers from requests

### Response Transformation

- Add security headers (X-Content-Type-Options, X-Frame-Options, etc.)
- Add correlation headers for tracing
- Remove internal service headers from responses
- Consistent error response format

## API Examples

### Public Endpoints (No Authentication)

<details>
<summary>Click to expand public endpoints</summary>

#### User Service

- `POST /api/v1/auth/login` - User login
- `POST /api/v1/auth/signup` - User registration
- `POST /api/v1/auth/refresh` - Refresh access token
- `GET /api/v1/auth/email/verify` - Verify email
- `POST /api/v1/auth/password/forgot` - Password reset request
- `GET /.well-known/jwks.json` - JWK Set for token validation

#### Billing Service

- `GET /api/v1/billing/subscription-plans/**` - List subscription plans
- `POST /api/v1/billing/webhooks/**` - Payment provider webhooks (Stripe, PayPal, etc.)

#### CRM Services

- `POST /api/v1/crm/webhooks/**` - External CRM provider webhooks

</details>

### Protected Endpoints (Requires JWT)

<details>
<summary>Click to expand protected endpoints</summary>

#### User Service

- `GET /api/v1/users/me` - Get current user profile
- `GET /api/v1/admin/users` - List users (ADMIN role)

#### Billing Service

- `GET /api/v1/features/my-features` - Get tenant features
- `GET /api/v1/features/enabled` - Get enabled features
- `GET /api/v1/billing/subscriptions/**` - Subscription operations
- `GET /api/v1/billing/payments/**` - Payment operations
- `GET /api/v1/billing/invoices/**` - Invoice operations
- `GET /api/v1/billing/payouts/**` - Payout operations
- `GET /api/v1/admin/billing/**` - Admin billing operations (ADMIN role)

#### CRM Services

- `GET /api/v1/leads/**` - Lead management (CRM_ACCESS role)
- `GET /api/v1/pipeline/**` - Pipeline management (CRM_ACCESS role)
- `GET /api/v1/contacts/**` - Contact management (CRM_ACCESS role)
- `GET /api/v1/companies/**` - Company management (CRM_ACCESS role)

</details>

### Monitoring Endpoints

- `/actuator/health` - Health status (Redis + user service health indicators)
- `/actuator/metrics` - Application metrics
- `/actuator/prometheus` - Prometheus metrics
- `/swagger-ui.html` - Aggregated API documentation

### Grafana Dashboard

A Grafana dashboard is available at `docs/monitoring/grafana-dashboard.json` providing real-time visibility into:

- Gateway health: uptime, request rate, error rate, p95 latency
- Routing metrics: request rate by route, response time percentiles (p50/p95/p99)
- JVM memory: heap/non-heap usage, GC pause time, thread count

The dashboard uses Prometheus as the data source and auto-refreshes every 30 seconds.

## Learning Points

This implementation serves as a reference for:

- Building reactive API gateways with Spring Cloud Gateway
- JWT validation and user context propagation
- Feature-based access control at the gateway level
- Multi-tenant request routing and context isolation
- Correlation ID tracking across services
- API versioning strategies (path and header-based)
- Request/response transformation patterns
- Reactive programming with Project Reactor
- Observability in distributed systems (tracing, metrics, structured logging)
- Type-safe configuration with Java records and Bean Validation

## Adapting for Your Domain

### API Gateway Patterns

- SaaS applications with tenant isolation and feature gating
- Microservices architectures requiring a unified entry point
- Mobile app backends with centralized authentication
- E-commerce platforms with multiple backend services

### Authentication Gateway

- Centralized authentication for microservices
- Token validation and context propagation
- Multi-tenant access control
- Public vs protected endpoint segregation

### Feature Access Control

- Subscription-based feature gating at the gateway
- Quota and limit enforcement before reaching services
- Usage tracking for billing analytics

### Request Transformation

- Header enrichment for downstream services
- Correlation ID generation for distributed tracing
- User and tenant context extraction and forwarding

## Integration with Downstream Services

### Consuming Gateway Context

<details>
<summary>Click to expand gateway context consumption example</summary>

Downstream services receive enriched headers from the gateway:

```java
@GetMapping("/protected")
public ResponseEntity<?> protectedEndpoint(
    @RequestHeader("X-User-ID") Long userId,
    @RequestHeader("X-Username") String username,
    @RequestHeader("X-User-Email") String email,
    @RequestHeader("X-User-Authorities") String authorities,
    @RequestHeader("X-Tenant-ID") String tenantId,
    @RequestHeader("X-Organization-ID") Long organizationId,
    @RequestHeader("X-Correlation-ID") String correlationId
) {
    List<String> authorityList = Arrays.asList(authorities.split(","));
    logger.info("Request from user {} (tenant: {}) correlation: {}", username, tenantId, correlationId);
    return ResponseEntity.ok(/* response */);
}
```

**Security Note**: The gateway sanitizes all incoming user/tenant context headers before processing. This prevents clients from spoofing user identity by injecting malicious headers. Only the gateway sets these headers after JWT validation.

</details>

### JWT Validation Configuration

<details>
<summary>Click to expand JWT validation configuration</summary>

Services can validate JWTs independently using the same JWK Set:

```yaml
spring:
    security:
        oauth2:
            resourceserver:
                jwt:
                    jwk-set-uri: http://iqscaffold-user-service:8080/.well-known/jwks.json
```

</details>

---

**Use this as a blueprint** for building reactive API gateways with intelligent routing, JWT authentication, feature-based access control, and multi-tenant support in your microservices architecture.
