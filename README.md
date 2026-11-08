# E-commerce Microservices Platform

An executable Spring Boot 3 / Spring Cloud reference platform demonstrating bounded contexts, service discovery, an API gateway, JWT authentication, Kafka messaging, a database-backed outbox, an order saga, and local observability.

## Architecture

| Component | Responsibility |
| --- | --- |
| API Gateway | Routes `/api/**`, validates user JWTs, applies role checks and Redis rate limits; registration and login are public |
| Config Server | Serves shared runtime configuration from its classpath-native repository |
| Discovery Server | Eureka service registry |
| Product Service | Catalog CRUD and Redis-backed product lookups |
| Order Service | Order history and orchestration saga |
| Inventory Service | Stock updates and idempotent reservations |
| Payment Service | Idempotent payment authorization and compensating refunds (simulated) |
| User Service | Account registration, BCrypt password hashes, one-hour HS256 access tokens |
| Notification Service | Idempotently records order events; logs a notification delivery placeholder |
| Shared contract | Versioned-in-repo `OrderEvent` payload |

Each bounded context owns a separate PostgreSQL database. The order saga prices an item from the catalog, reserves stock, authorizes payment, and confirms the order. A failure triggers payment refund and/or inventory release. Order state changes and their outbox records are committed atomically; the publisher retries Kafka delivery, and the notification consumer deduplicates by event ID. Kafka's default listener error handler retries failed events and routes exhausted deliveries to `orders.events.DLT`.

Spring Cloud Sleuth is not compatible with Spring Boot 3; services use Micrometer Tracing with the Brave bridge and Zipkin exporter instead. SpringDoc exposes OpenAPI UI at `/swagger-ui.html` and JSON at `/v3/api-docs`.
The gateway accepts the stable `/api/v1/...` route prefix and keeps `/api/...` as a compatibility alias.

## Run locally

Requirements: Docker Compose v2 and Java/Maven only if building outside containers.

```powershell
Copy-Item .env.example .env
docker compose up --build
```

The first build packages each service image, so allow a few minutes. The service endpoints become available after their health checks pass. To stop the stack, run `docker compose down`; add `-v` only when intentionally deleting local database and Elasticsearch data.

| Endpoint | Purpose |
| --- | --- |
| `http://localhost:8080` | Gateway |
| `http://localhost:8761` | Eureka |
| `http://localhost:8888` | Config Server |
| `http://localhost:9411` | Zipkin |
| `http://localhost:5601` | Kibana |
| `http://localhost:9090` | Prometheus |
| `http://localhost:3000` | Grafana (`admin` / configured local password) |
| `http://localhost:9200` | Elasticsearch |

For local API exploration and SpringDoc, each domain service is additionally bound to loopback only: product `8081`, order `8082`, payment `8083`, user `8084`, inventory `8085`, and notification `8086`. These direct development ports bypass gateway JWT enforcement; use the gateway (`8080`) for the authenticated flow.

### Try the order flow (PowerShell)

Register and log in; use the returned bearer token for protected gateway routes:

```powershell
$body = @{ email = "buyer@example.com"; password = "local-password-123" } | ConvertTo-Json
Invoke-RestMethod -Method Post http://localhost:8080/api/users/register -ContentType "application/json" -Body $body
$login = Invoke-RestMethod -Method Post http://localhost:8080/api/users/login -ContentType "application/json" -Body $body
$headers = @{ Authorization = "Bearer $($login.accessToken)" }
```

Create a product and add initial stock using the loopback-only development ports, then place an order through the authenticated gateway. Catalog writes require an administrator token; direct loopback ports are used here only for local bootstrap:

```powershell
$productBody = @{ sku = "BOOK-001"; name = "Example Book"; price = 19.99 } | ConvertTo-Json
$product = Invoke-RestMethod -Method Post http://localhost:8081/api/products -ContentType "application/json" -Body $productBody
$stockBody = @{ sku = "BOOK-001"; quantity = 25 } | ConvertTo-Json
Invoke-RestMethod -Method Post http://localhost:8085/api/inventory/stock -ContentType "application/json" -Body $stockBody
$orderBody = @{ productId = $product.id; quantity = 2 } | ConvertTo-Json
$order = Invoke-RestMethod -Method Post http://localhost:8080/api/orders -Headers $headers -ContentType "application/json" -Body $orderBody
```

The payment API is a test double; no real charge is made. Notification delivery currently records and logs events rather than contacting an email/SMS provider. Newly registered accounts receive `ROLE_USER`; promote a trusted account to `ROLE_ADMIN` in the user database to allow catalog writes through the gateway.

## Build and test

```powershell
mvn --batch-mode verify
```

Build one image from the repository root with `docker build --build-arg SERVICE=product-service -t local/ecommerce-product-service:1.0.0 .`.

## Kubernetes / Helm

The chart deploys the nine Spring applications, Services, readiness/liveness probes, an API ingress, and CPU-based HPAs. PostgreSQL, Kafka, Redis, Zipkin, and the metrics server are external dependencies in Kubernetes; set their endpoints in `helm/ecommerce/values.yaml` or a production values file. Create the referenced secret before installation (use a strong random JWT secret and a database password):

```powershell
kubectl create secret generic ecommerce-secrets --from-literal=jwt-secret="<strong-random-secret-at-least-32-bytes>" --from-literal=postgres-password="<database-password>"
helm upgrade --install ecommerce-platform ./helm/ecommerce
```

Publish the local images to a registry and override `imagePrefix` / `imageTag` before deploying to a cluster. Configure ingress TLS, network policies, managed secrets, database migrations, durable Kafka topics, and a production payment/email provider before exposing this reference implementation to real traffic. Hibernate schema auto-update and the development credentials in `.env.example` are for local learning only.

## Development notes

- See the endpoint request/response schemas in each service's generated SpringDoc page.
- `database/init/01-create-databases.sh` provisions one local database per domain in the shared PostgreSQL container; it is only run when the data volume is initialized.
- Spring Boot 3 requires Java 17 or newer. The CI workflow runs the Maven reactor build and tests on Java 17.
