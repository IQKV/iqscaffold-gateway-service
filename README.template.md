# Project Name 🌐

<!-- TEMPLATE: This README.template.md is a starter template. Copy parts into your real README.md and replace placeholders. -->

<details>
  <summary><strong>How to use this template (click to expand)</strong></summary>

1. Rename the title above to your service name and optionally add a logo right below it.
2. Add badges (build, license) under the title.
3. Fill each section below with your actual service content.
4. Update the filter chain table to reflect your actual filters and their `getOrder()` values.
5. Update the downstream headers table to match the claims your JWT carries.
6. Update the routes table to reflect your actual `application.yml` routes and profile-based configs.
7. Update the environment variables table to match your `IqScaffoldProperties` bindings.
8. Remove this guidance block after you finish customizing.

</details>

- Add your service logo.
- Write a short introduction — what the gateway does, which platform it belongs to, and what cross-cutting concerns it owns.
- If you are using badges, add them here.

<details>
  <summary><strong>Badge examples (optional)</strong></summary>

- Build: <code>![CI](https://img.shields.io/github/actions/workflow/status/ORG/REPO/build-java-project.yml?label=CI)</code>
- License: <code>![License](https://img.shields.io/github/license/ORG/REPO)</code>
- Java: <code>![Java](https://img.shields.io/badge/java-21-blue)</code>
- Spring Boot: <code>![Spring Boot](https://img.shields.io/badge/spring--boot-3.x-brightgreen)</code>

</details>

## About

Describe the gateway's responsibilities in plain language:

- What services does it front?
- What cross-cutting concerns does it own (auth, feature gating, tenant isolation, locale, versioning)?
- What does it produce for downstream services (headers, exchange attributes)?

## Quick Links

- [API Documentation](./docs/api/README.md)
- [Architecture Overview](./docs/architecture/README.md)
- [Deployment Guide](./docs/deployment/README.md)
- [Contributing Guidelines](.github/CONTRIBUTING.md)

## Key Features

- **Reactive Gateway**: Spring Cloud Gateway with WebFlux — non-blocking I/O throughout the filter chain
- **JWT Authentication**: RS256 validation via JWK Set URI; public paths bypass auth via `iqscaffold.gateway.security.public-paths`
- **Header Sanitization**: Strips spoofable context headers from incoming requests before JWT processing
- **Context Propagation**: Extracts user/tenant claims from the validated JWT and forwards them as typed headers
- **Feature Access Control**: Validates tenant subscription features before routing; fail-open on billing service errors
- **Route-Level Authority Enforcement**: `UnifiedMicroserviceAccessFilter` checks `PlatformConfigurationProperties.protectedRoutes` per path pattern
- **Redis Caching**: Reactive Redis (`ReactiveStringRedisTemplate`) for feature context and token caching
- **Locale Extraction**: `LocaleExtractionFilter` propagates `Accept-Language` / `X-Locale` to downstream services
- **API Versioning**: `ApiVersionRoutingFilter` handles path-based and header-based version routing
- **Profile-Based Routes**: CRM and billing routes activated via Spring profiles (`crm`, `billing`)
- **Aggregated Swagger UI**: SpringDoc proxies downstream `/api-docs` endpoints through the gateway
- **Observability**: OpenTelemetry tracing (OTLP), Prometheus metrics, structured JSON logging with MDC

## Prerequisites

- Java 21 (Eclipse Temurin)
- Maven 3.9+
- Node.js >= 22.x & pnpm (git hooks, if configured)
- Docker & Docker Compose
- Redis (provided via `compose.yaml` for local dev)

## Quick Start

```bash
# Clone the repository
git clone https://github.com/ORG/REPO.git
cd REPO

# Start dependencies (Redis + mock downstream services)
docker compose up -d

# Run the application (local + crm + billing profiles)
./mvnw spring-boot:run -Dspring-boot.run.profiles=local,crm,billing
# → Gateway API:  http://localhost:8080
# → Actuator:     http://localhost:8081/actuator/health
# → Swagger UI:   http://localhost:8080/swagger-ui.html
```

## Filter Chain

Filters execute in order. Each `GlobalFilter` is `Ordered` — lower numbers run first on the request path, last on the response path.

| Filter                            | Responsibility                                                                    |
| --------------------------------- | --------------------------------------------------------------------------------- |
| `CorrelationIdFilter`             | Generate or propagate `X-Correlation-ID`; store in exchange attributes            |
| `LocaleExtractionFilter`          | Extract locale from `Accept-Language` / `X-Locale`; propagate downstream          |
| `TenantExtractionFilter`          | Extract tenant context from JWT claims or `X-Tenant-ID` header                    |
| `JwtAuthenticationFilter`         | Validate JWT (RS256 via JWKS); extract user context                               |
| `UnifiedMicroserviceAccessFilter` | Enforce route-level authority requirements from `PlatformConfigurationProperties` |
| `ApiVersionRoutingFilter`         | Handle path-based and header-based API version routing                            |
| `FeatureAccessFilter`             | Validate tenant subscription features; block with `403` if feature disabled       |
| `RequestTransformationFilter`     | Enrich downstream headers with user/tenant/feature context                        |
| `ResponseTransformationFilter`    | Add security response headers; echo `X-Correlation-ID` to client                  |

> Update this table to reflect your actual filters. Document the `getOrder()` value for each if order matters for your use case.

## Downstream Headers

After the filter chain, downstream services receive the following headers on every authenticated request:

| Header               | Source                 | Description                                              |
| -------------------- | ---------------------- | -------------------------------------------------------- |
| `X-User-ID`          | JWT claim              | User identifier                                          |
| `X-Username`         | JWT claim              | Username                                                 |
| `X-User-Email`       | JWT claim              | User email address                                       |
| `X-User-Authorities` | JWT claim              | Comma-separated authority list (e.g. `ADMIN,CRM_ACCESS`) |
| `X-User-Permissions` | JWT claim              | Comma-separated permission list                          |
| `X-Tenant-ID`        | JWT claim / header     | Tenant identifier                                        |
| `X-Organization-ID`  | JWT claim              | Organization identifier                                  |
| `X-Correlation-ID`   | Generated / propagated | Request correlation ID                                   |
| `X-Gateway-Version`  | Static config          | Gateway version string                                   |

> Update this table to match the claims in your JWT and the headers your `RequestTransformationFilter` actually sets.

**Security note**: The gateway strips all context headers from the incoming client request before JWT processing. Only the gateway sets them after successful validation.

## Routes

Routes are defined in `application.yml` and profile-specific Java config classes:

| Route ID                     | URI                   | Predicate                     | Profile   | Auth                 |
| ---------------------------- | --------------------- | ----------------------------- | --------- | -------------------- |
| `user-service-auth`          | `USER_SERVICE_URI`    | `Path=/api/v1/auth/**`        | default   | Public               |
| `user-service-public-signup` | `USER_SERVICE_URI`    | `Path=/api/v1/public/signup`  | default   | Public               |
| `user-service-profile`       | `USER_SERVICE_URI`    | `Path=/api/v1/users/me/**`    | default   | JWT                  |
| `user-service-admin`         | `USER_SERVICE_URI`    | `Path=/api/v1/admin/**`       | default   | JWT `ADMIN`          |
| `user-service-jwks`          | `USER_SERVICE_URI`    | `Path=/.well-known/jwks.json` | default   | Public               |
| `billing-*`                  | `BILLING_SERVICE_URI` | `Path=/api/v1/billing/**`     | `billing` | JWT `BILLING_ACCESS` |
| `crm-leads`                  | `CRM_SERVICE_URI`     | `Path=/api/v1/leads/**`       | `crm`     | JWT `CRM_ACCESS`     |
| `crm-contacts`               | `CRM_SERVICE_URI`     | `Path=/api/v1/contacts/**`    | `crm`     | JWT `CRM_ACCESS`     |

> Replace with your actual routes. Profile-based routes are configured in `config/module/BillingRouteConfig.java` and `config/module/CrmRouteConfig.java`.

## Route-Level Authority Enforcement

Protected route patterns and their required authorities are configured in `application.yml` under `iqscaffold.platform.security.routeProtection.protectedRoutes`:

```yaml
iqscaffold:
    platform:
        security:
            routeProtection:
                protectedRoutes:
                    "/api/*/crm/**": ["CRM_ACCESS", "ADMIN", "SUPER_ADMIN"]
                    "/api/*/billing/**": ["BILLING_ACCESS", "ADMIN", "SUPER_ADMIN"]
                    "/api/*/admin/**": ["ADMIN", "SUPER_ADMIN"]
                    "/api/**": ["API_ACCESS", "USER", "ADMIN", "SUPER_ADMIN"]
            accessControl:
                bypassAuthorities: ["SUPER_ADMIN"]
```

> Update patterns and authority lists to match your domain. `SUPER_ADMIN` bypasses all route checks by default.

## Feature Access Control

Feature-to-endpoint mappings are configured under `iqscaffold.gateway.feature-access.mappings`. The `FeatureValidationService` calls the billing service to check tenant feature entitlements; on error it fails open (requests proceed).

```yaml
iqscaffold:
    gateway:
        feature-access:
            enabled: true
            mappings:
                - path: /api/v1/leads/**
                  feature: CRM_LEADS
                - path: /api/v1/pipeline/**
                  feature: CRM_PIPELINE
```

> Add mappings for each feature-gated endpoint. Remove this section if your gateway does not integrate with a billing/feature service.

## Environment Variables

| Variable                                         | Default                                                | Description                                                 |
| ------------------------------------------------ | ------------------------------------------------------ | ----------------------------------------------------------- |
| `IQSCAFFOLD_CACHE_REDIS_HOST`                    | `localhost`                                            | Redis host                                                  |
| `IQSCAFFOLD_CACHE_REDIS_PORT`                    | `6379`                                                 | Redis port                                                  |
| `IQSCAFFOLD_CACHE_REDIS_PASSWORD`                | _(empty)_                                              | Redis password                                              |
| `IQSCAFFOLD_CACHE_REDIS_DATABASE`                | `0`                                                    | Redis database index                                        |
| `IQSCAFFOLD_GATEWAY_ROUTING_USER_SERVICE_URI`    | `http://iqscaffold-user-service`                       | User service base URI                                       |
| `IQSCAFFOLD_GATEWAY_ROUTING_BILLING_SERVICE_URI` | `http://iqscaffold-billing-service`                    | Billing service base URI                                    |
| `USER_SERVICE_JWK_URI`                           | `http://iqscaffold-user-service/.well-known/jwks.json` | JWK Set URI for JWT validation                              |
| `JWT_SECRET_KEY`                                 | `change-me-in-production`                              | JWT secret (HS256 fallback — use RS256 in production)       |
| `IQSCAFFOLD_GATEWAY_SECURITY_USER_SERVICE_URL`   | `http://iqscaffold-user-service`                       | User service URL for token validation                       |
| `IQSCAFFOLD_OBSERVABILITY_TRACING_ENDPOINT`      | `http://localhost:4317`                                | OpenTelemetry OTLP endpoint                                 |
| `API_STRIP_COUNT`                                | `0`                                                    | Path segments to strip (0 = no stripping, 1 = strip `/api`) |
| `SPRING_PROFILES_ACTIVE`                         | `local,crm,billing`                                    | Active Spring profiles                                      |

> Add rows for each downstream service URI you add. Copy `.env.example` to `.env.local` and fill in values.

## Maven Commands

```bash
# Build and test (skip Checkstyle during development)
./mvnw clean verify -Dcheckstyle.skip=true

# Run tests only
./mvnw test -Dcheckstyle.skip=true

# Explicit Checkstyle check
./mvnw checkstyle:check

# Coverage report → target/site/jacoco/index.html
./mvnw jacoco:report

# Production build
./mvnw clean package -Pproduction
```

## Docker

```bash
# Build image
docker build -t ORG/REPO:latest .

# Run with dependencies (Redis + mock services)
docker compose up -d
```

The Dockerfile uses a multi-stage build: Maven compiles in `eclipse-temurin:21-jdk-alpine`, the runtime stage uses `eclipse-temurin:21-jre-alpine` with a non-root `appuser`, layered JAR extraction, and Reactor Netty tuning (`ioWorkerCount`, `maxConnections`).

## Monitoring

| Endpoint                   | Description                                                   |
| -------------------------- | ------------------------------------------------------------- |
| `GET /actuator/health`     | Liveness + readiness probes (includes Redis health indicator) |
| `GET /actuator/metrics`    | Application metrics                                           |
| `GET /actuator/prometheus` | Prometheus scrape endpoint                                    |
| `GET /swagger-ui.html`     | Aggregated API documentation                                  |
| `GET /api-docs`            | Gateway OpenAPI spec                                          |

## Project Structure

```
src/main/java/com/iqscaffold/gatewayservice/
├── config/
│   ├── IqScaffoldProperties.java           # @ConfigurationProperties root record
│   ├── PlatformConfigurationProperties.java # Route protection + access control config
│   ├── SecurityConfiguration.java          # WebFlux security, JWT converter, public paths
│   ├── RedisConfiguration.java             # Reactive Redis setup
│   ├── WebClientConfig.java                # WebClient for downstream calls
│   ├── DynamicOpenApiRoutesConfiguration.java # Aggregated Swagger UI routes
│   └── module/
│       ├── BillingRouteConfig.java         # Billing routes (billing profile)
│       ├── BillingOpenApiConfig.java       # Billing OpenAPI aggregation
│       └── CrmRouteConfig.java             # CRM routes (crm profile)
├── filter/
│   ├── CorrelationIdFilter.java            # X-Correlation-ID generation/propagation
│   ├── LocaleExtractionFilter.java         # Locale extraction and propagation
│   ├── TenantExtractionFilter.java         # Tenant context extraction
│   ├── ApiVersionRoutingFilter.java        # API version routing
│   ├── FeatureAccessFilter.java            # Subscription feature gating
│   ├── RequestTransformationFilter.java    # Header enrichment
│   └── ResponseTransformationFilter.java   # Security headers + correlation echo
├── security/
│   ├── JwtAuthenticationFilter.java        # JWT validation + user context extraction
│   ├── UnifiedMicroserviceAccessFilter.java # Route-level authority enforcement
│   └── JwtClaimNames.java                  # JWT claim name constants
└── service/
    ├── FeatureValidationService.java        # Calls billing service for feature checks
    ├── FeatureUsageTrackingService.java     # Async usage tracking for billing analytics
    ├── ApiVersionExtractor.java             # Extracts API version from path/header
    └── ApiPrefixService.java               # Handles /api prefix stripping
```

---

<details>
  <summary><strong>✅ Pre-publish checklist (remove in final README)</strong></summary>

- [ ] Title updated and logo added
- [ ] Badges added (CI, license)
- [ ] About section completed
- [ ] Filter chain table reflects actual filters
- [ ] Downstream headers table matches JWT claims and `RequestTransformationFilter`
- [ ] Routes table reflects actual `application.yml` routes and profile configs
- [ ] Route protection patterns updated for your domain
- [ ] Feature access mappings documented (or section removed if not applicable)
- [ ] Environment variables table is complete
- [ ] Project structure tree updated if packages differ
- [ ] Links verified (docs, external resources)
- [ ] Guidance blocks removed before publishing

</details>

---

## 🧩 Boilerplate Architecture

- **Rendering model**: Reactive — Spring Cloud Gateway + WebFlux, non-blocking I/O end-to-end
- **Security**: Spring Security OAuth2 Resource Server; JWT RS256 via JWKS; public path list in `IqScaffoldProperties`; route-level authority enforcement via `PlatformConfigurationProperties`
- **Feature gating**: `FeatureAccessFilter` → `FeatureValidationService` → billing service; fail-open on errors; async usage tracking via `FeatureUsageTrackingService`
- **Caching**: Reactive Redis (`ReactiveStringRedisTemplate`) for feature context and token caching; configurable TTL and key prefix
- **Configuration**: Type-safe nested `@ConfigurationProperties` records (`IqScaffoldProperties`); Bean Validation annotations; profile-based route activation
- **Observability**: OpenTelemetry tracing (OTLP export); Micrometer + Prometheus; structured JSON logging (Logstash encoder) with MDC (userId, tenantId, correlationId, traceId)
- **GitHub Integration**: Issue templates, labels, Dependabot, and CI workflows
- **Quality Tools**: Checkstyle, JaCoCo, ArchUnit, Spring Modulith tests, Reactor Test, embedded Redis for tests

> See [AGENTS.md](AGENTS.md) for detailed project structure, DDD patterns, and AI agent guidelines.
