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

| Environment | Namespace      | Purpose                     |
| ----------- | -------------- | --------------------------- |
| Test        | `iqkv-sit-env` | Feature branch testing      |
| Staging     | `iqkv-uat-env` | Pre-production validation   |
| Production  | `iqkv-prd-env` | Live production environment |

### Automated Deployment (CI/CD)

#### Drone Pipeline Overview

<details>
<summary>📋 Pipeline Stages</summary>

The service uses Drone CI/CD pipeline with 10 stages:

1. **VerifyCode** - Code quality, tests, static analysis
2. **PublishArtifacts** - Maven artifacts to Nexus
3. **PublishDockerImage** - Container images to registry
4. **DeployWorkInProgress** - WIP branch auto-deployment
5. **RollbackWorkInProgress** - WIP rollback
6. **PromoteFeatureDeployment** - Feature branch promotion
7. **RollbackFeatureDeployment** - Feature rollback
8. **PromoteDeployment** - Release promotion
9. **RollbackDeployment** - Release rollback
10. **ReleasePackage** - Automated version management

</details>

<details>
<summary>🔐 Required Drone Secrets</summary>

| Secret Name                       | Purpose                              | Used In                                    |
| --------------------------------- | ------------------------------------ | ------------------------------------------ |
| `NEXUS_DEPLOYER_USERNAME`         | Nexus repository authentication      | Artifact publishing, dependency resolution |
| `NEXUS_DEPLOYER_PASSWORD`         | Nexus repository authentication      | Artifact publishing, dependency resolution |
| `SONAR_HOST`                      | SonarQube server URL                 | Static code analysis                       |
| `SONAR_TOKEN`                     | SonarQube authentication token       | Static code analysis                       |
| `SLACK_WEBHOOK`                   | Slack notifications webhook URL      | Build status notifications                 |
| `GITHUB_API_ACCESS_TOKEN`         | GitHub API access for releases       | Release creation, changelog generation     |
| `SVC_CONTAINER_REGISTRY_USERNAME` | Container registry authentication    | Docker image publishing                    |
| `SVC_CONTAINER_REGISTRY_PASSWORD` | Container registry authentication    | Docker image publishing                    |
| `HELM_CHARTS_REPOSITORY`          | Helm charts repository URL           | Kubernetes deployments                     |
| `INFRA_REDIS_PASSWORD`            | Redis cache password                 | Rate limiting, caching, session storage    |
| `JWT_SECRET_KEY`                  | JWT symmetric validation key (HS256) | Authentication and authorization           |

</details>

#### Branch Deployment Strategy

| Branch Type | Auto Deploy | Manual Promote | Target Environment |
| ----------- | ----------- | -------------- | ------------------ |
| `wip`       | ✅ Dev      | -              | Dev                |
| `feature/*` | -           | ✅ Test        | Test               |
| `dev`       | -           | ✅ Staging     | Staging            |
| Tags        | -           | ✅ Production  | Production         |

#### Deployment Commands

The pipeline uses these Helm commands for deployment:

<details>
<summary>Helm Commands</summary>

```bash
# Development (WIP branches)
helm upgrade --install --atomic --wait --timeout 5m iqscaffold-gateway-service ./ \
  --values ./values.yaml \
  --values ./values-test.yaml \
  --set image.tag=wip \
  --set infraServices.redis.password=${INFRA_REDIS_PASSWORD} \
  --set config.gateway.security.jwt.secret=${JWT_SECRET_KEY} \
  --namespace iqkv-sit-env

# Production (Tagged releases)
helm upgrade --install --atomic --wait --timeout 5m iqscaffold-gateway-service ./ \
  --values ./values.yaml \
  --values ./values-prd.yaml \
  --set image.tag=${DRONE_TAG} \
  --set infraServices.redis.password=${INFRA_REDIS_PASSWORD} \
  --set config.gateway.security.jwt.secret=${JWT_SECRET_KEY} \
  --namespace iqkv-prd-env
```

</details>

### Manual Deployment

#### Quick Start

```bash
# Clone Helm charts
git clone <HELM_CHARTS_REPOSITORY> charts
cd charts/IQKV/iqscaffold-gateway-service

# Deploy to development
helm upgrade --install gateway-service ./ \
  --values values-sit.yaml \
  --set infraServices.redis.password="your-redis-password" \
  --set config.gateway.security.jwt.secret="your-secure-symmetric-key" \
  --namespace iqkv-sit-env \
  --create-namespace
```

#### Environment-Specific Deployments

#### Development

```bash
helm upgrade --install gateway-service ./ \
  --values values-sit.yaml \
  --set infraServices.redis.password="${REDIS_PASSWORD}" \
  --set config.gateway.security.jwt.secret="${JWT_SECRET_KEY}" \
  --namespace iqkv-sit-env \
  --create-namespace
```

#### Production

```bash
helm upgrade --install gateway-service ./ \
  --values values-prd.yaml \
  --set infraServices.redis.password="${REDIS_PASSWORD}" \
  --set config.gateway.security.jwt.secret="${JWT_SECRET_KEY}" \
  --namespace iqkv-prd-env \
  --create-namespace
```

### Configuration

#### Drone CI Secrets

The following secrets must be configured in Drone CI for automated deployments:

```bash
# Infrastructure Secrets
drone secret add --repository IQKV/iqscaffold-gateway-service --name INFRA_REDIS_PASSWORD --data "your-redis-password"

# Application Secrets
drone secret add --repository IQKV/iqscaffold-gateway-service --name JWT_SECRET_KEY --data "your-secure-symmetric-key"

# Repository and Registry Secrets (already configured)
drone secret add --repository IQKV/iqscaffold-gateway-service --name HELM_CHARTS_REPOSITORY --data "your-helm-charts-repo-url"
drone secret add --repository IQKV/iqscaffold-gateway-service --name NEXUS_DEPLOYER_USERNAME --data "your-nexus-username"
drone secret add --repository IQKV/iqscaffold-gateway-service --name NEXUS_DEPLOYER_PASSWORD --data "your-nexus-password"
```

#### Environment Variable Mapping

The Helm chart maps Drone CI secrets to application environment variables:

| Drone Secret           | Helm --set Parameter                 | Application Environment Variable  |
| ---------------------- | ------------------------------------ | --------------------------------- |
| `INFRA_REDIS_PASSWORD` | `infraServices.redis.password`       | `IQSCAFFOLD_CACHE_REDIS_PASSWORD` |
| `JWT_SECRET_KEY`       | `config.gateway.security.jwt.secret` | `JWT_SECRET_KEY`                  |

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
    kubectl logs deployment/iqscaffold-gateway-service -n iqkv-sit-env
    ```

2. **Redis Connection Issues**

    ```bash
    # Check Redis connectivity
    kubectl exec -it deployment/iqscaffold-gateway-service -n iqkv-sit-env -- \
      redis-cli -h foundation-infra-redis-master.iqkv-sit-env.svc.cluster.local ping
    ```

3. **JWT Validation Errors**

    ```bash
    kubectl logs deployment/iqscaffold-gateway-service -n iqkv-sit-env | grep "JWT"
    ```

4. **Rate Limiting Issues**

    ```bash
    kubectl logs deployment/iqscaffold-gateway-service -n iqkv-sit-env | grep "rate"
    ```

5. **Check Configuration**

    ```bash
    kubectl describe configmap iqscaffold-gateway-service-config -n iqkv-sit-env
    kubectl describe secret iqscaffold-gateway-service-secrets -n iqkv-sit-env
    ```

6. **Test Health Endpoints**
    ```bash
    kubectl port-forward deployment/iqscaffold-gateway-service 8081:8081 -n iqkv-sit-env
    curl http://localhost:8081/actuator/health
    ```

#### Missing Secrets Diagnosis

If deployments fail due to missing secrets, check:

```bash
# List all secrets in namespace
kubectl get secrets -n iqkv-sit-env

# Check specific secret content
kubectl get secret iqscaffold-gateway-service-secrets -n iqkv-sit-env -o yaml

# Verify Drone CI secrets are configured
drone secret ls --repository IQKV/iqscaffold-gateway-service
```

#### Circuit Breaker Status

```bash
# Check circuit breaker metrics
kubectl port-forward deployment/iqscaffold-gateway-service 8081:8081 -n iqkv-sit-env
curl http://localhost:8081/actuator/metrics/resilience4j.circuitbreaker.state
```

#### Gateway Routing Testing

```bash
# Test gateway routing through port-forward
kubectl port-forward deployment/iqscaffold-gateway-service 8080:8080 -n iqkv-sit-env

# Test user service routing
curl -H "Authorization: Bearer your-jwt-token" http://localhost:8080/api/v1/users/profile

# Test health check routing
curl http://localhost:8080/actuator/health
```

#### Rollback

```bash
# Rollback to previous version
helm rollback iqscaffold-gateway-service -n iqkv-prd-env

# Or uninstall completely
helm uninstall iqscaffold-gateway-service -n iqkv-prd-env
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
