# Spring Boot Microservices with Consul & ELK Stack

A production-ready microservices architecture demonstrating service discovery with Consul, centralized logging with ELK Stack, and load balancing across multiple service instances.

## 🚀 Technologies

### Core Stack
- **Spring Boot** 3.2.3
- **Java** 21 (Amazon Corretto via SDKMAN)
- **Groovy** 4.0.16
- **Maven** 3.9+

### Frameworks & Libraries
- **Spring Cloud** 2023.0.0 (Leyton)
- **Spring Data JPA** with Hibernate 6.4.x
- **Spring Cloud Consul** for service discovery
- **Thymeleaf** 3.x for server-side rendering
- **Spock Framework** 2.4-M4-groovy-4.0 for testing

### Infrastructure & Observability
- **Consul** 1.16.0 - Service registry and health checking
- **MySQL** 8.0 - Production database (shared across service instances)
- **H2** - File-based database for local development (with AUTO_SERVER=TRUE)
- **Elasticsearch** 7.10.2 - Log storage and full-text search
- **Logstash** 7.10.2 - Log aggregation and processing pipeline
- **Kibana** 7.10.2 - Log visualization and analytics
- **Prometheus** v2.45.0 - Metrics collection and time-series storage
- **Grafana** 10.0.3 - Metrics visualization and dashboards
- **Docker** & **Docker Compose** - Containerization and orchestration

## 📋 Prerequisites

- **Java 21** (Amazon Corretto recommended)
  ```bash
  sdk install java 21.0.6-amzn
  sdk default java 21.0.6-amzn
  ```
- **Maven 3.9+**
  ```bash
  mvn -version
  ```
- **Docker Desktop** with 4GB memory minimum
- **SDKMAN** (optional but recommended for Java version management)
  ```bash
  curl -s "https://get.sdkman.io" | bash
  ```

## 🏗️ Architecture

### Module Structure

```
consul-elk-sample/
├── pom.xml                          # Parent POM (Spring Boot 3.2.3)
├── spring-boot-common/              # Shared components
│   ├── domain/
│   │   └── Person.groovy            # JPA entity with Jakarta validation
│   └── filter/
│       └── RequestLoggingFilter.groovy # MDC logging filter
├── spring-boot-service/             # Backend REST API (3 instances)
│   ├── BackApplication.groovy
│   ├── PersonController.groovy      # REST endpoints
│   ├── PersonService.groovy         # Business logic
│   ├── PersonRepository.groovy      # Spring Data JPA
│   └── MetricsConfig.groovy         # 21 custom metric beans
└── spring-boot-front/               # Frontend web application
    ├── FrontApplication.groovy
    ├── PersonController.groovy      # Web UI controllers
    ├── MdcPropagationInterceptor.groovy # Distributed tracing
    └── MetricsConfig.groovy         # 11 custom metric beans
```

### Service Discovery & Communication Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    Consul (Service Registry)                 │
│                         :8500                                │
└─────────────────────────────────────────────────────────────┘
          ↑                                    ↑
     (register)                           (discover)
          │                                    │
    ┌─────┴────────────────────────────────────┴──────┐
    │                                                  │
    │  Frontend (person-front)                        │
    │  :8080                                          │
    │  ┌─────────────────────────────────┐            │
    │  │ @LoadBalanced RestTemplate      │            │
    │  │ (Client-Side Load Balancing)    │            │
    │  └─────────────────────────────────┘            │
    │           │ round-robin distribution             │
    │           ↓                                      │
    │  ┌──────────┬──────────┬──────────┐             │
    │  │ Service-1│ Service-2│ Service-3│             │
    │  │ :8081    │ :8082    │ :8083    │             │
    │  └──────────┴──────────┴──────────┘             │
    │           │                                      │
    │           ↓                                      │
    │  ┌────────────────────────────────┐             │
    │  │  MySQL 8.0 (Shared Database)   │             │
    │  │  :3306                          │             │
    │  └────────────────────────────────┘             │
    └──────────────────────────────────────────────────┘
```

### Observability Architecture

```
┌────────────────────────────────────────────────────────────┐
│                    LOGGING PIPELINE                         │
├────────────────────────────────────────────────────────────┤
│                                                             │
│  All Services (MDC + JSON)                                 │
│         │                                                   │
│         ↓ (TCP :4560)                                      │
│  ┌──────────────┐    ┌──────────────┐    ┌─────────────┐ │
│  │  Logstash    │ → │Elasticsearch │ → │   Kibana    │ │
│  │   :4560      │    │    :9200     │    │   :5601     │ │
│  └──────────────┘    └──────────────┘    └─────────────┘ │
│  • Log aggregation   • Log storage      • Log analytics  │
│  • JSON parsing      • Full-text search • Visualization  │
│                                                             │
└────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────┐
│                   METRICS PIPELINE                          │
├────────────────────────────────────────────────────────────┤
│                                                             │
│  All Services (/actuator/prometheus)                       │
│         │                                                   │
│         ↓ (scrape every 15s)                               │
│  ┌──────────────┐    ┌──────────────┐                     │
│  │ Prometheus   │ → │   Grafana    │                     │
│  │   :9090      │    │    :3000     │                     │
│  └──────────────┘    └──────────────┘                     │
│  • Metrics storage   • 3 Dashboards:                       │
│  • PromQL queries    • Person Frontend                     │
│  • Consul SD         • Business Metrics                    │
│  • 30-day retention  • JVM & Infrastructure                │
│                                                             │
└────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────┐
│              DISTRIBUTED TRACING (MDC)                      │
├────────────────────────────────────────────────────────────┤
│                                                             │
│  Request → Frontend (request_id: UUID)                     │
│              │                                              │
│              ↓ (X-Request-ID header)                       │
│           Backend (propagates request_id)                  │
│              │                                              │
│              ↓ (logs with same request_id)                 │
│           Kibana (query: request_id:"UUID")                │
│           → See complete request flow!                     │
│                                                             │
└────────────────────────────────────────────────────────────┘
```

### Configuration Structure

All Docker service configurations are centralized in `docker-configs/`:

```
docker-configs/
├── prometheus-config/
│   └── prometheus.yml              # Scrape config with Consul service discovery
├── grafana-config/
│   ├── provisioning/
│   │   ├── datasources/
│   │   │   └── prometheus.yml      # Auto-provision Prometheus datasource
│   │   └── dashboards/
│   │       └── dashboard.yml       # Auto-load dashboard configuration
│   └── dashboards/
│       ├── person-front-dashboard.json         # Frontend metrics
│       ├── person-service-dashboard.json       # Business metrics
│       └── jvm-metrics-dashboard.json          # JVM & infrastructure
└── logstash-config/
    └── logstash-tcp-input.conf     # Log ingestion pipeline (TCP :4560)
```

**Why Centralized?**
- Single location for all infrastructure configs
- Easier to version control and manage
- Clear separation from application code
- Simplified Docker Compose volume mounts

## 🔨 Building the Project

### Clean Build
```bash
mvn clean package
```

### Build Specific Module
```bash
mvn clean package -pl spring-boot-service -am
```

### Skip Tests
```bash
mvn clean package -DskipTests
```

### Run Tests
```bash
mvn test
```

**Test Coverage**: 56 Spock tests (100% passing)
- 10 domain validation tests (common module)
- 12 frontend controller tests (front module)
- 34 backend tests (service module):
  - 12 service layer tests
  - 11 controller tests
  - 11 integration tests

## 🐳 Running with Docker Compose

### Start All Services
```bash
docker compose up -d --build
```

This starts 11 containers:
- 3x Backend service instances (person-service-client)
- 1x Frontend web app (person-front)
- 1x MySQL 8.0
- 1x Consul
- 1x Prometheus
- 1x Grafana
- 1x Elasticsearch
- 1x Logstash
- 1x Kibana

### View Logs
```bash
# All services
docker compose logs -f

# Specific service
docker compose logs -f service-1
docker compose logs -f front
docker compose logs -f mysql

# Tail last 100 lines
docker compose logs --tail=100 service-1
```

### Check Service Status
```bash
docker compose ps
```

### Stop All Services
```bash
docker compose down
```

### Stop and Remove Volumes
```bash
docker compose down -v
```

## 🌐 Access Points

| Service | URL | Description |
|---------|-----|-------------|
| **Frontend** | http://localhost:8080 | Web UI for person management |
| **Backend API** | http://localhost:8081/people | REST API (service-1) |
| **Backend API** | http://localhost:8082/people | REST API (service-2) |
| **Backend API** | http://localhost:8083/people | REST API (service-3) |
| **Consul UI** | http://localhost:8500/ui | Service registry dashboard |
| **Prometheus** | http://localhost:9090 | Metrics collection and queries |
| **Grafana** | http://localhost:3000 | Metrics visualization (admin/admin) |
| **Kibana** | http://localhost:5601 | Log analytics dashboard |
| **Elasticsearch** | http://localhost:9200 | Search engine API |
| **H2 Console** | http://localhost:8081/console | H2 database console (local only) |

## 🗄️ Database Configuration

### MySQL (Docker)
- **Database**: consul-example
- **User**: consul
- **Password**: example
- **Port**: 3306
- **Version**: 8.0.45

### H2 (Local Development)
- **Type**: File-based
- **Location**: ~/consul-example
- **Driver**: org.h2.Driver
- **Console**: http://localhost:8081/console
- **Schema**: Auto-created on startup

## 🔗 API Endpoints

### Person REST API

#### List All Persons
```bash
GET /people
curl http://localhost:8081/people
```

#### Get Person by ID
```bash
GET /people/{id}
curl http://localhost:8081/people/1
```

#### Create Person
```bash
POST /people
curl -X POST http://localhost:8081/people \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John",
    "lastname": "Doe",
    "email": "john.doe@example.com"
  }'
```

#### Update Person
```bash
PUT /people
curl -X PUT http://localhost:8081/people \
  -H "Content-Type: application/json" \
  -d '{
    "id": 1,
    "name": "Jane",
    "lastname": "Doe",
    "email": "jane.doe@example.com"
  }'
```

#### Delete Person
```bash
DELETE /people/{id}
curl -X DELETE http://localhost:8081/people/1
```

## 🔍 Verifying the Setup

### Check Consul Services
```bash
curl http://localhost:8500/v1/catalog/services
```

Expected output:
```json
{
  "consul": [],
  "person-front": [],
  "person-service-client": []
}
```

### Check Service Health
```bash
curl http://localhost:8500/v1/health/service/person-service-client
```

### Verify Load Balancing
```bash
# Hit different service instances
for i in {1..5}; do
  curl -s http://localhost:8080/people | jq '.[] | .name'
done
```

### Check Elasticsearch Indices
```bash
curl http://localhost:9200/_cat/indices?v
```

### View Application Logs in Kibana
1. Open http://localhost:5601
2. Go to "Discover"
3. Create index pattern: `logstash-*`
4. Explore logs with filters and queries

## 🧪 Running Tests Locally

### All Tests
```bash
mvn test
```

### Specific Test Class
```bash
mvn test -Dtest=PersonServiceImplSpec
```

### Integration Tests Only
```bash
mvn test -Dtest="*IntegrationSpec"
```

### With Coverage Report
```bash
mvn clean verify jacoco:report
```

## 🛠️ Local Development

### Run Backend Service Locally
```bash
cd spring-boot-service
mvn spring-boot:run
```

**Note**: Consul and MySQL must be running (via Docker Compose or locally)

### Run Frontend Locally
```bash
cd spring-boot-front
mvn spring-boot:run
```

### Run with Specific Profile
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=docker
```

### Debug Mode
```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
```

## 📊 Monitoring & Observability

### Health Checks

All services expose Spring Boot Actuator endpoints:

```bash
# Service health
curl http://localhost:8081/actuator/health

# Service info
curl http://localhost:8081/actuator/info

# Prometheus metrics
curl http://localhost:8081/actuator/prometheus
```

### Prometheus Metrics Collection

**Prometheus** (http://localhost:9090) collects metrics from all service instances via Consul service discovery:

```bash
# View all available metrics
curl http://localhost:8081/actuator/prometheus | grep person_

# Query metrics in Prometheus
curl 'http://localhost:9090/api/v1/query?query=person_operations_total'
```

**Custom Application Metrics** (32 total beans defined in `MetricsConfig.groovy`):

**Backend Service Metrics** (`spring-boot-service` - 21 beans):
- `person.operations.total` - Counter for CRUD operations (create, read, update, delete, list) with success/failed result tags
- `person.service.execution.time` - Timer for service method execution times by operation type
- `person.repository.total` - Gauge tracking current person count in database
- `person.api.requests.total` - Counter for REST API endpoint calls by endpoint and HTTP method
- `person.validation.errors.total` - Counter for validation failures by field name
- `person.bootstrap.load.total` - Counter for initial data loading events
- `person.bootstrap.execution.time` - Timer for bootstrap data load duration
- `person.operation.duration` - Histogram for operation duration distribution (p50, p95, p99)

**Frontend Metrics** (`spring-boot-front` - 11 beans):
- `person.frontend.requests.total` - Counter for frontend operations (list, view, create, update, delete)
- `person.frontend.backend.errors.total` - Counter for backend communication failures
- `person.frontend.page.render.time` - Timer for page render performance with percentile tracking

**Architecture**: 
- Metrics are defined as `@Bean` in centralized `MetricsConfig` classes
- Injected via `@Autowired` + `@Qualifier` for type safety
- All metrics use Micrometer registry
- Exposed at `/actuator/prometheus` endpoint
- Scraped by Prometheus every 15 seconds

### Grafana Dashboards

**Grafana** (http://localhost:3000) provides real-time visualization with 3 pre-configured dashboards:

- **Default credentials**: admin / admin
- **Datasource**: Prometheus (auto-provisioned on startup)
- **Auto-refresh**: Configurable (5s, 10s, 30s, 1m)

**Pre-loaded Dashboards**:

1. **Person Frontend Service Dashboard**
   - Frontend operation counters (list, view, create, update, delete)
   - Success rate gauge
   - Backend communication errors
   - Page render time with percentiles (p50, p95, p99)
   - Operations over time visualization

2. **Person Service - Business Metrics Dashboard**
   - Total persons in database (gauge)
   - CRUD operations rate by type
   - API request rate by endpoint
   - Operation duration percentiles
   - Validation errors by field
   - Bootstrap data loading metrics

3. **Person Service - JVM & Infrastructure Dashboard**
   - JVM heap memory usage (used vs max)
   - JVM non-heap memory (metaspace, code cache)
   - Garbage collection pause time
   - Thread count (live, daemon)
   - HikariCP connection pool utilization
   - Process and system CPU usage
   - HTTP request rate and response times

**Dashboard Features**:
- Time range selector (Last 5m, 15m, 1h, 6h, 24h, 7d, custom)
- Auto-refresh with configurable intervals
- Panel zoom and inspect
- Query editor with PromQL syntax
- Legend with min/max/avg/current values
- Export to PNG/PDF

**Access Dashboards**:
1. Login at http://localhost:3000 (admin/admin)
2. Click "Dashboards" icon (☰) in left sidebar
3. Select dashboard from list
4. Use time picker and refresh controls

**Create Custom Dashboard**:
1. Navigate to Dashboards → New Dashboard
2. Add Panel → Select Prometheus datasource
3. Use PromQL queries:
   - `rate(person_operations_total[5m])` - Operations per second
   - `person_repository_count` - Current person count
   - `histogram_quantile(0.95, person_operation_duration_bucket)` - p95 latency

### Consul Health Dashboard

Visit http://localhost:8500/ui to view:
- All registered services
- Service instance health status
- Service discovery configuration

### Log Analysis with Kibana

1. **Access Kibana**: http://localhost:5601
2. **Create Index Pattern**:
   - Navigate to Management → Index Patterns
   - Create pattern: `logstash-*`
   - Select timestamp field: `@timestamp`
3. **View Logs**:
   - Go to Discover
   - Filter by service name, log level, or custom queries
4. **Example Queries**:
   ```
   service_name: "person-service-client"
   level: "ERROR"
   message: "exception"
   ```

## 🚨 Troubleshooting

### Services Won't Start

**Issue**: Port already in use
```bash
# Find and kill process using port 8081
lsof -ti:8081 | xargs kill -9
```

**Issue**: Docker daemon not running
```bash
# Start Docker Desktop
open -a Docker
```

### Consul Connection Refused

**Issue**: Consul not started before services
```bash
# Restart with proper dependency order
docker compose down
docker compose up -d consul
sleep 5
docker compose up -d
```

### Database Connection Errors

**Issue**: MySQL not ready
```bash
# Check MySQL health
docker compose ps mysql

# View MySQL logs
docker compose logs mysql

# MySQL takes ~30 seconds to initialize on first run
```

### Test Failures

**Issue**: Bootstrap data loading in tests
- Tests use `@Profile("!test")` to prevent Bootstrap component from loading data
- Integration tests use H2 in-memory database

**Issue**: Port conflicts during tests
- Spring Boot tests use random ports: `webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT`

## 📚 Project Features

### Service Discovery
- ✅ Automatic service registration with Consul
- ✅ Client-side load balancing with @LoadBalanced
- ✅ Health check monitoring
- ✅ Dynamic service discovery (no hardcoded URLs)

### Centralized Logging
- ✅ JSON-formatted logs via logstash-logback-encoder
- ✅ Log aggregation from all service instances
- ✅ Full-text search capabilities
- ✅ Real-time log streaming

### Data Persistence
- ✅ Spring Data JPA repositories
- ✅ Jakarta Bean Validation constraints
- ✅ Multi-instance shared database
- ✅ Automatic schema management

### Testing
- ✅ Unit tests with Spock mocks
- ✅ Integration tests with @SpringBootTest
- ✅ Domain validation tests
- ✅ 100% test pass rate

## 🔄 Development Workflow

1. **Make Code Changes**
   ```bash
   # Edit Groovy files in your IDE
   ```

2. **Run Tests**
   ```bash
   mvn test
   ```

3. **Build Application**
   ```bash
   mvn clean package
   ```

4. **Rebuild Docker Images**
   ```bash
   docker compose up -d --build
   ```

5. **Verify Changes**
   ```bash
   # Check logs
   docker compose logs -f service-1
   
   # Test API
   curl http://localhost:8081/people
   
   # View in Kibana
   open http://localhost:5601
   ```

## 📖 Documentation

**Quick Links**: [Documentation Index](docs/INDEX.md) | [Acronyms Glossary](docs/INDEX.md#acronyms--terminology)

### Complete Guides

| Guide | Description | Lines | Status |
|-------|-------------|-------|--------|
| **[Documentation Index](docs/INDEX.md)** | Central hub with file descriptions and acronyms glossary | 539 | ✅ |
| **[Docker Commands](docs/docker-commands.md)** | Complete Docker & Docker Compose reference | 600 | ✅ |
| **[Consul Service Discovery](docs/consul-service-discovery.md)** | Service registry, health checks, load balancing | 1,490 | ✅ |
| **[ELK Stack Logging](docs/elk-stack-logging.md)** | Centralized logging with Elasticsearch, Logstash, Kibana (with 4 Kibana screenshots) | 2,278 | ✅ |
| **[Metrics & Monitoring](docs/metrics-monitoring.md)** | Application metrics, Prometheus, Grafana dashboards (with 5 Grafana screenshots) | 2,133 | ✅ |

### Quick Start by Role

**New Developers**: Start with [Docker Commands](docs/docker-commands.md) → [Consul Guide](docs/consul-service-discovery.md) → [ELK Logging](docs/elk-stack-logging.md)

**Operations/DevOps**: Start with [Docker Commands](docs/docker-commands.md) → [Metrics & Monitoring](docs/metrics-monitoring.md) → [ELK Logging](docs/elk-stack-logging.md)

**Troubleshooting**: Check [Documentation Index](docs/INDEX.md#quick-start) for issue-specific guide recommendations

## 🤝 Contributing

### Code Style
- Use Groovy for all new code
- Follow Spring Boot best practices
- Write Spock tests for all new features
- Use @LoadBalanced for inter-service communication

### Commit Messages
```bash
# Format: <type>: <description>
# Types: feat, fix, refactor, docs, test, chore

feat: add new person search endpoint
fix: resolve service discovery timeout
refactor: improve logging configuration
docs: update README with new endpoints
test: add integration tests for delete operation
```

## 📝 License

This project is open source and available for educational purposes.

## 🙋 Support

For issues or questions:
1. Check this README
2. Review logs: `docker compose logs -f`
3. Check Consul UI: http://localhost:8500/ui
4. Verify health: `curl http://localhost:8081/actuator/health`

---

**Built with** ❤️ **using Spring Boot, Groovy, Consul, and ELK Stack**
