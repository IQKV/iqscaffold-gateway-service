## 📜 Deployment Guide

### Overview

The IQ Scaffold Gateway Service is deployed using Helm charts and automated CI/CD pipelines. The service provides API gateway functionality with routing, JWT authentication, rate limiting, circuit breakers, and multi-service orchestration for the microservices platform.

### Prerequisites

- Kubernetes 1.19+
- Helm 3.2.0+
- External infrastructure services (Redis for rate limiting and caching)
- User Service (for JWT validation)
- Downstream microservices (Billing, CRM services)

### Environments

| Environment | Namespace                   | Purpose                      |
| ----------- | --------------------------- | ---------------------------- |
| Dev         | `iqscaffold-dev-env`        | Development and WIP branches |
| Test        | `iqscaffold-test-env`       | Feature branch testing       |
| Staging     | `iqscaffold-staging-env`    | Pre-production validation    |
| Production  | `iqscaffold-production-env` | Live production environment  |

### Automated Deployment (CI/CD)

#### Drone Pipeline Overview

The service uses a comprehensive Drone CI/CD pipeline with 10 stages:

1. **VerifyCode** - Code quality, tests, static analysis
2. **PublishArtifacts** - Maven artifacts to Nexus
3. **PublishDockerImage** - Container images to registry
4. **DeployWorkInProgressOnDev** - WIP branch auto-deployment
5. **RollbackWorkInProgressOnDev** - WIP rollback
6. **PromoteFeatureDeployment** - Feature branch promotion
7. **RollbackFeatureDeployment** - Feature rollback
8. **PromoteDeployment** - Release promotion
9. **RollbackDeployment** - Release rollback
10. **ReleasePackage** - Automated version management

#### Branch Deployment Strategy

| Branch Type | Auto Deploy | Manual Promote | Target Environment |
| ----------- | ----------- | -------------- | ------------------ |
| `wip`       | ✅ Dev      | -              | Dev                |
| `feature/*` | -           | ✅ Test        | Test               |
| `dev`       | -           | ✅ Staging     | Staging            |
| Tags        | -           | ✅ Production  | Production         |

#### Deployment Commands

The pipeline uses these Helm commands for deployment:

```bash
# Development (WIP branches)
helm upgrade --install --atomic --wait --timeout 5m iqscaffold-gateway-service ./ \
  --values ./values.yaml \
  --values ./values-dev.yaml \
  --set image.tag=wip \
  --set externalServices.redis.password=${REDIS_PASSWORD} \
  --set config.gateway.security.jwt.secret=${JWT_SECRET_KEY} \
  --namespace iqscaffold-dev-env

# Production (Tagged releases)
helm upgrade --install --atomic --wait --timeout 5m iqscaffold-gateway-service ./ \
  --values ./values.yaml \
  --values ./values-production.yaml \
  --set image.tag=${DRONE_TAG} \
  --set externalServices.redis.password=${REDIS_PASSWORD} \
  --set config.gateway.security.jwt.secret=${JWT_SECRET_KEY} \
  --namespace iqscaffold-production-env
```

### Manual Deployment

#### Quick Start

```bash
# Clone Helm charts
git clone <HELM_CHARTS_REPOSITORY> charts
cd charts/IQKV/iqscaffold-gateway-service

# Deploy to development
helm upgrade --install gateway-service ./ \
  --values values-dev.yaml \
  --set config.gateway.security.jwt.secret="your-jwt-secret" \
  --namespace iqscaffold-dev-env \
  --create-namespace
```

#### Environment-Specific Deployments

#### Development

```bash
helm upgrade --install gateway-service ./ \
  --values values-dev.yaml \
  --namespace iqscaffold-dev-env \
  --create-namespace
```

#### Production

```bash
helm upgrade --install gateway-service ./ \
  --values values-production.yaml \
  --set externalServices.redis.password="${REDIS_PASSWORD}" \
  --set config.gateway.security.jwt.secret="${JWT_SECRET_KEY}" \
  --namespace iqscaffold-production-env \
  --create-namespace
```

### Configuration

#### Required Secrets

| Secret         | Environment Variable | Required | Description               |
| -------------- | -------------------- | -------- | ------------------------- |
| JWT Secret     | `JWT_SECRET_KEY`     | ✅       | JWT validation secret key |
| Redis Password | `REDIS_PASSWORD`     | ⚠️       | Cache and rate limiting   |

#### External Services

The service connects to these external infrastructure components and downstream services:

- **Redis**: Rate limiting, caching, and session storage
- **User Service**: JWT validation and user authentication
- **Billing Service**: Payment and subscription routing (optional)
- **CRM Services**: Lead, Pipeline, and Contact service routing (optional)

#### Service Configuration

| Setting        | Dev      | Production       |
| -------------- | -------- | ---------------- |
| Replicas       | 1        | 3                |
| CPU Request    | 250m     | 1000m            |
| Memory Request | 384Mi    | 1Gi              |
| Autoscaling    | Disabled | 3-10 replicas    |
| Ingress        | Enabled  | Enabled with TLS |
| Monitoring     | Disabled | Enabled          |

#### Gateway Features

- **Routing**: API prefix routing with configurable path stripping
- **Authentication**: JWT-based authentication with User Service integration
- **Rate Limiting**: Redis-backed rate limiting per client
- **Circuit Breakers**: Resilience4j circuit breakers for downstream services
- **CORS**: Configurable CORS policies for web applications
- **Observability**: Distributed tracing and metrics collection

### Monitoring & Health Checks

#### Health Endpoints

- **Liveness**: `/actuator/health/liveness` (port 8081)
- **Readiness**: `/actuator/health/readiness` (port 8081)
- **Metrics**: `/actuator/prometheus` (port 8081)

#### Monitoring Stack

Production deployments include:

- Prometheus ServiceMonitor
- Alerting rules for service health, latency, and circuit breaker status
- Grafana dashboards for gateway metrics and routing performance

#### Gateway-Specific Alerts

- High latency (95th percentile > 2 seconds)
- Circuit breaker open states
- High error rates on downstream services

### Troubleshooting

#### Common Issues

1. **Downstream Service Connection Failures**

   ```bash
   kubectl logs deployment/iqscaffold-gateway-service -n iqscaffold-dev-env
   ```

2. **JWT Validation Errors**

   ```bash
   kubectl logs deployment/iqscaffold-gateway-service -n iqscaffold-dev-env | grep "JWT"
   ```

3. **Rate Limiting Issues**

   ```bash
   kubectl logs deployment/iqscaffold-gateway-service -n iqscaffold-dev-env | grep "rate"
   ```

4. **Check Configuration**

   ```bash
   kubectl describe configmap iqscaffold-gateway-service-config -n iqscaffold-dev-env
   ```

5. **Test Health Endpoints**
   ```bash
   kubectl port-forward deployment/iqscaffold-gateway-service 8081:8081 -n iqscaffold-dev-env
   curl http://localhost:8081/actuator/health
   ```

#### Circuit Breaker Status

```bash
# Check circuit breaker metrics
kubectl port-forward deployment/iqscaffold-gateway-service 8081:8081 -n iqscaffold-dev-env
curl http://localhost:8081/actuator/metrics/resilience4j.circuitbreaker.state
```

#### Rollback

```bash
# Rollback to previous version
helm rollback iqscaffold-gateway-service -n iqscaffold-production-env

# Or uninstall completely
helm uninstall iqscaffold-gateway-service -n iqscaffold-production-env
```

### Security

- All sensitive values passed via `--set` flags
- TLS enabled in production with automatic certificate management
- Network policies restrict pod communication
- Non-root container execution
- Read-only root filesystem in production
- JWT signature validation for all authenticated routes
- CORS policies configured for allowed origins
- Rate limiting to prevent abuse
