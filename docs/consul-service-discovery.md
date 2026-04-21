# Consul Service Discovery Guide

**Version**: 1.0  
**Last Updated**: April 20, 2026  
**Consul Version**: Latest (1.x)  
**Spring Cloud Version**: 2023.0.0 (Leyton)

---

## Table of Contents

1. [What is Consul?](#what-is-consul)
2. [Consul Architecture in This Project](#consul-architecture-in-this-project)
3. [Service Registration](#service-registration)
4. [Service Discovery](#service-discovery)
5. [Health Checks](#health-checks)
6. [Consul UI Walkthrough](#consul-ui-walkthrough)
7. [Troubleshooting](#troubleshooting-consul-issues)
8. [Best Practices](#best-practices)
9. [Advanced Topics](#advanced-topics)

---

## What is Consul?

**Consul** is a service mesh solution by HashiCorp that provides service discovery, health checking, configuration management, and network infrastructure automation for cloud-native applications.

### Key Features

#### 🔍 Service Discovery
Services register themselves with Consul and discover other services through DNS or HTTP APIs. No hardcoded IP addresses or ports needed.

#### 💓 Health Checking
Consul continuously monitors service health through configurable health checks. Unhealthy instances are automatically removed from the available pool.

#### 🗄️ Key/Value Store
Distributed configuration storage for dynamic application configuration without redeployment.

#### 🔒 Service Mesh (Consul Connect)
Secure service-to-service communication with automatic TLS encryption and identity-based authorization.

#### 🌍 Multi-Datacenter Support
Native support for multiple datacenters with cross-DC service discovery and failover.

### Why Consul for This Project?

We chose Consul over other service discovery solutions for several reasons:

| Feature | Consul | Eureka | Zookeeper |
|---------|--------|--------|-----------|
| **Service Discovery** | ✅ DNS + HTTP | ✅ HTTP only | ✅ Custom clients |
| **Health Checking** | ✅ Built-in | ✅ Client-side | ⚠️ Basic |
| **Multi-DC Support** | ✅ Native | ❌ No | ⚠️ Complex |
| **Key/Value Store** | ✅ Yes | ❌ No | ✅ Yes |
| **Service Mesh** | ✅ Consul Connect | ❌ No | ❌ No |
| **Maintenance** | ✅ Active | ⚠️ Netflix OSS | ✅ Apache |
| **Learning Curve** | 🟢 Easy | 🟢 Easy | 🔴 Steep |
| **Spring Cloud** | ✅ Excellent | ✅ Excellent | ⚠️ Limited |

**Our Decision Factors**:
- **Production-ready**: Widely used by enterprises (PayPal, Twitch, GE)
- **Spring Boot integration**: First-class Spring Cloud support
- **Future-proof**: Service mesh capabilities for advanced scenarios
- **Operational simplicity**: Easy to deploy and maintain
- **Strong community**: Active development and support

---

## Consul Architecture in This Project

### Deployment Model

We use a **single-node Consul server** for development and testing. In production, you would typically run a **3 or 5 node cluster** for high availability.

```
┌─────────────────────────────────────────────────────────┐
│                    Consul Server                         │
│                   (localhost:8500)                       │
│                                                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐ │
│  │   Service    │  │    Health    │  │   KV Store   │ │
│  │   Registry   │  │   Checking   │  │  (optional)  │ │
│  └──────────────┘  └──────────────┘  └──────────────┘ │
└─────────────────────────────────────────────────────────┘
           ↑                    ↑                    ↑
           │                    │                    │
  ┌────────┴────────┐  ┌────────┴────────┐  ┌────────┴────────┐
  │  person-front   │  │  service-1      │  │  service-2      │
  │  (port 8080)    │  │  (port 8081)    │  │  (port 8082)    │
  └─────────────────┘  └─────────────────┘  └─────────────────┘
           │                    │                    │
           └────────────────────┴────────────────────┘
                            MySQL 8.0
```

### Service Registry Structure

Consul maintains a registry of all services with the following information:

```json
{
  "ID": "person-service-client-8081-a9eaa6eb8d6b9141404e6596695ff7d9",
  "Node": "docker-container",
  "Address": "service-1",
  "Datacenter": "dc1",
  "TaggedAddresses": {
    "lan": "172.20.0.5",
    "wan": "172.20.0.5"
  },
  "Meta": {
    "secure": "false"
  },
  "ServiceID": "person-service-client-8081-a9eaa6eb8d6b9141404e6596695ff7d9",
  "ServiceName": "person-service-client",
  "ServiceAddress": "service-1",
  "ServicePort": 8081,
  "ServiceEnableTagOverride": false,
  "CreateIndex": 12,
  "ModifyIndex": 14
}
```

**Key Fields**:
- `ServiceName`: Logical name used for discovery (e.g., "person-service-client")
- `ServiceAddress`: Hostname or IP where service is reachable
- `ServicePort`: Port number for the service
- `ServiceID`: Unique identifier for this instance
- `CreateIndex`/`ModifyIndex`: Consul internal versioning

### Docker Compose Configuration

Our Consul server runs as a Docker container:

```yaml
consul:
  image: hashicorp/consul:latest
  container_name: "dev-consul"
  environment:
    - "CONSUL_BIND_INTERFACE=eth0"
  ports:
    - "8301:8301"   # LAN serf
    - "8400:8400"   # RPC (deprecated)
    - "8500:8500"   # HTTP API + UI
    - "8600:53/udp" # DNS
  command: "agent -server -bootstrap -ui -client=0.0.0.0 -bind='{{ GetInterfaceIP \"eth0\" }}'"
```

**Command Breakdown**:
- `agent`: Start Consul agent
- `-server`: Run in server mode (not client mode)
- `-bootstrap`: Single-node cluster (not recommended for production)
- `-ui`: Enable web UI
- `-client=0.0.0.0`: Allow external HTTP API connections
- `-bind`: Bind to container's eth0 interface

---

## Service Registration

### Spring Cloud Consul Integration

Our services automatically register with Consul on startup using Spring Cloud Consul.

#### Maven Dependency

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-consul-discovery</artifactId>
</dependency>
```

#### Enable Discovery Client

In the main application class:

```groovy
@SpringBootApplication(scanBasePackages = ['io.github.joxebus'])
@EnableDiscoveryClient  // ← Enables Consul integration
class BackApplication {
    static void main(String[] args) {
        SpringApplication.run(BackApplication, args)
    }
}
```

### Configuration

#### application.yml (Backend Service)

```yaml
spring:
  application:
    name: person-service-client  # ← Service name in Consul
  cloud:
    consul:
      host: localhost              # Consul server address (local dev)
      port: 8500                   # Consul HTTP API port
      discovery:
        enabled: true              # Enable service registration
        health-check-path: /actuator/health  # Health check endpoint
        health-check-interval: 10s           # Check every 10 seconds
        instance-id: ${spring.application.name}:${SERVER_PORT:8081}:${random.value}
        hostname: ${HOSTNAME:localhost}      # Hostname for registration
        prefer-ip-address: false             # Use hostname, not IP
        fail-fast: false                     # Don't fail startup if Consul unreachable
        register: true                       # Auto-register on startup

server:
  port: ${SERVER_PORT:8081}
```

#### application-docker.yml (Docker Profile)

```yaml
spring:
  cloud:
    consul:
      host: consul  # ← Docker service name (DNS resolution)
      port: 8500
      discovery:
        enabled: true
        health-check-path: /actuator/health
        health-check-interval: 10s
        instance-id: ${spring.application.name}:${SERVER_PORT:8081}:${random.value}
        prefer-ip-address: false
```

#### application.yml (Frontend)

```yaml
spring:
  application:
    name: person-front  # ← Different service name
  cloud:
    consul:
      host: localhost
      port: 8500
      discovery:
        enabled: true
        health-check-path: /actuator/health
        health-check-interval: 10s
        instance-id: ${spring.application.name}:${SERVER_PORT:8080}:${random.value}
        hostname: ${HOSTNAME:localhost}
```

### Configuration Properties Explained

| Property | Purpose | Example Value |
|----------|---------|---------------|
| `spring.application.name` | Service name in registry | `person-service-client` |
| `spring.cloud.consul.host` | Consul server hostname | `consul` (Docker) or `localhost` |
| `spring.cloud.consul.port` | Consul HTTP API port | `8500` |
| `discovery.enabled` | Enable/disable registration | `true` |
| `discovery.health-check-path` | Endpoint for health checks | `/actuator/health` |
| `discovery.health-check-interval` | How often to check health | `10s` |
| `discovery.instance-id` | Unique instance identifier | `person-service-client-8081-abc123` |
| `discovery.hostname` | Hostname for registration | `service-1` |
| `discovery.prefer-ip-address` | Use IP instead of hostname | `false` |
| `discovery.fail-fast` | Fail if Consul unavailable | `false` (dev), `true` (prod) |
| `discovery.register` | Auto-register on startup | `true` |

### Service Instance ID Pattern

The `instance-id` uniquely identifies each service instance:

```
${spring.application.name}:${SERVER_PORT}:${random.value}
```

**Example**: `person-service-client:8081:951df138a529c53d69e4e32f75e6534f`

**Why this pattern?**:
- `spring.application.name`: Groups instances of the same service
- `SERVER_PORT`: Distinguishes instances on different ports
- `random.value`: Ensures uniqueness even on same host/port

### Registration Lifecycle

#### 1. Application Startup
```
Application starts → Spring Boot initializes → Consul Discovery Client activated
                                                         ↓
                                              Service registers with Consul
                                                         ↓
                                              Initial health check performed
                                                         ↓
                                              Service marked as "passing" or "critical"
```

#### 2. Normal Operation
```
Service running → Consul performs periodic health checks (every 10s)
                                  ↓
                    Health check HTTP GET /actuator/health
                                  ↓
                    200 OK → "passing" status maintained
                    Non-200 → "critical" status, removed from pool
```

#### 3. Graceful Shutdown
```
Application shutdown signal received → Spring Boot shutdown hooks execute
                                                    ↓
                                        Service deregisters from Consul
                                                    ↓
                                        Consul removes from service registry
                                                    ↓
                                        No more traffic routed to instance
```

#### 4. Crash/Ungraceful Shutdown
```
Application crashes → Health checks start failing
                                  ↓
                    After 3 consecutive failures (configurable)
                                  ↓
                    Service marked as "critical"
                                  ↓
                    Removed from available service pool
                                  ↓
                    After deregister_critical_service_after (default: 30m)
                                  ↓
                    Service fully removed from registry
```

### Verifying Registration

#### Using Consul HTTP API
```bash
# List all services
curl http://localhost:8500/v1/catalog/services

# Expected output:
# {
#   "consul": [],
#   "person-front": [],
#   "person-service-client": []
# }

# Get service instances
curl http://localhost:8500/v1/catalog/service/person-service-client | jq .

# Check health status
curl http://localhost:8500/v1/health/service/person-service-client | jq .
```

#### Using Consul CLI (if installed)
```bash
# List all services
consul catalog services

# Get service details
consul catalog nodes -service=person-service-client

# Check service health
consul health service person-service-client
```

#### Using Consul UI
Open http://localhost:8500/ui/dc1/services

You should see:
- `person-front` (1 instance)
- `person-service-client` (3 instances)

---

## Service Discovery

### How Services Find Each Other

Instead of hardcoding URLs like `http://192.168.1.100:8081`, services use **logical names** and let Consul resolve them to actual instances.

### @LoadBalanced RestTemplate

Spring Cloud provides `@LoadBalanced` annotation that integrates with Consul for service discovery and client-side load balancing.

#### Configuration (Frontend)

```groovy
package io.github.joxebus.config

import org.springframework.cloud.client.loadbalancer.LoadBalanced
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

@Configuration
class RestTemplateConfig {

    @Bean
    @LoadBalanced  // ← Magic happens here
    RestTemplate restTemplate() {
        new RestTemplate()
    }
}
```

#### Usage in Controller

```groovy
@Autowired
RestTemplate restTemplate

def getAllPersons() {
    // Use logical service name, not IP/hostname
    def url = "http://person-service-client/people"
    
    List<Person> persons = restTemplate.getForObject(url, List.class)
    return persons
}

def createPerson(Person person) {
    def url = "http://person-service-client/people"
    
    Person created = restTemplate.postForObject(url, person, Person.class)
    return created
}
```

### How @LoadBalanced Works

```
1. Application calls: restTemplate.getForObject("http://person-service-client/people", List.class)
                                                      ↓
2. LoadBalancerInterceptor intercepts the request
                                                      ↓
3. Queries Consul for instances of "person-service-client"
                                                      ↓
4. Consul returns: [service-1:8081, service-2:8082, service-3:8083]
                                                      ↓
5. LoadBalancer selects instance (round-robin by default)
                                                      ↓
6. Request sent to selected instance: http://service-1:8081/people
                                                      ↓
7. Response returned to caller
```

### Load Balancing Strategies

Spring Cloud LoadBalancer provides different strategies:

#### Round Robin (Default)
Distributes requests evenly across all healthy instances:
```
Request 1 → service-1:8081
Request 2 → service-2:8082
Request 3 → service-3:8083
Request 4 → service-1:8081  (back to first)
Request 5 → service-2:8082
...
```

#### Random
Randomly selects an instance:
```
Request 1 → service-2:8082
Request 2 → service-1:8081
Request 3 → service-2:8082
Request 4 → service-3:8083
...
```

#### Custom Configuration

To change load balancing strategy:

```groovy
@Configuration
class LoadBalancerConfiguration {

    @Bean
    ReactorLoadBalancer<ServiceInstance> randomLoadBalancer(
            Environment environment,
            LoadBalancerClientFactory loadBalancerClientFactory) {
        
        String name = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME)
        return new RandomLoadBalancer(
            loadBalancerClientFactory.getLazyProvider(name, ServiceInstanceListSupplier.class),
            name
        )
    }
}
```

### Service Discovery with DNS

Consul also provides DNS-based service discovery (alternative to HTTP API):

```bash
# Query Consul DNS server
dig @127.0.0.1 -p 8600 person-service-client.service.consul

# Returns:
# person-service-client.service.consul. 0 IN A 172.20.0.5
# person-service-client.service.consul. 0 IN A 172.20.0.6
# person-service-client.service.consul. 0 IN A 172.20.0.7
```

**DNS Query Format**: `<service-name>.service.<datacenter>.consul`

**Advantages**:
- No code changes needed
- Works with any HTTP client
- Language-agnostic

**Disadvantages**:
- No client-side load balancing
- Less control over instance selection
- Requires DNS resolver configuration

### Handling Failures

#### Retry Logic

Configure retry behavior:

```yaml
spring:
  cloud:
    loadbalancer:
      retry:
        enabled: true
        max-attempts-on-next-service-instance: 2
        max-attempts-on-same-service-instance: 1
```

#### Circuit Breaker Integration

Combine with Resilience4j for circuit breaker pattern:

```xml
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
</dependency>
```

```groovy
@CircuitBreaker(name = "personService", fallbackMethod = "getPersonsFallback")
def getAllPersons() {
    restTemplate.getForObject("http://person-service-client/people", List.class)
}

def getPersonsFallback(Exception e) {
    log.error("Circuit breaker activated: ${e.message}")
    return []  // Return empty list as fallback
}
```

---

## Health Checks

### Spring Boot Actuator Health Endpoint

All our services expose a health endpoint via Spring Boot Actuator:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

#### Health Endpoint Configuration

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: when-authorized
```

#### Health Check Response

```bash
curl http://localhost:8081/actuator/health

# Response:
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "MySQL",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 500068036608,
        "free": 198273302528,
        "threshold": 10485760,
        "exists": true
      }
    },
    "ping": {
      "status": "UP"
    }
  }
}
```

### Consul Health Check Configuration

Consul performs HTTP health checks against the actuator endpoint:

```yaml
spring:
  cloud:
    consul:
      discovery:
        health-check-path: /actuator/health
        health-check-interval: 10s
        health-check-timeout: 5s
        health-check-critical-timeout: 30m
```

**Health Check Properties**:

| Property | Description | Default | Our Value |
|----------|-------------|---------|-----------|
| `health-check-path` | Endpoint to check | `/actuator/health` | `/actuator/health` |
| `health-check-interval` | Time between checks | `10s` | `10s` |
| `health-check-timeout` | Max time for check | `10s` | `5s` |
| `health-check-critical-timeout` | Time before deregister | `30m` | `30m` |

### Health Check Lifecycle

```
Service starts
    ↓
Initial health check (immediate)
    ↓
┌─────────────────────────────────────┐
│ Periodic Health Check Loop          │
│                                      │
│ Every 10 seconds:                   │
│   1. HTTP GET /actuator/health      │
│   2. Wait up to 5 seconds           │
│   3. Check response status          │
│      - 200 OK → "passing"           │
│      - 429 (too many requests) → OK │
│      - Other/timeout → "critical"   │
│                                      │
│ If "critical" for 30 minutes:       │
│   → Deregister service              │
└─────────────────────────────────────┘
```

### Health Check States

| State | Description | Visible in Pool | Auto-Deregister |
|-------|-------------|----------------|-----------------|
| **passing** | Health check succeeds | ✅ Yes | No |
| **warning** | Custom warning status | ✅ Yes | No |
| **critical** | Health check fails | ❌ No | After 30m |

### Custom Health Indicators

Add custom health checks:

```groovy
package io.github.joxebus.health

import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Component

@Component
class DatabaseConnectionHealthIndicator implements HealthIndicator {

    @Autowired
    DataSource dataSource

    @Override
    Health health() {
        try {
            // Try to get a connection
            Connection connection = dataSource.getConnection()
            connection.close()
            
            return Health.up()
                .withDetail("database", "MySQL")
                .withDetail("status", "Connected")
                .build()
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.message)
                .build()
        }
    }
}
```

### Viewing Health Check History

#### Via Consul API
```bash
# Get service health with checks
curl http://localhost:8500/v1/health/service/person-service-client?pretty

# Output includes health check history:
[
  {
    "Node": {
      "ID": "...",
      "Node": "docker-container",
      "Address": "172.20.0.5"
    },
    "Service": {
      "ID": "person-service-client-8081-...",
      "Service": "person-service-client",
      "Port": 8081
    },
    "Checks": [
      {
        "Node": "docker-container",
        "CheckID": "serfHealth",
        "Name": "Serf Health Status",
        "Status": "passing",
        "Output": "Agent alive and reachable"
      },
      {
        "Node": "docker-container",
        "CheckID": "service:person-service-client-8081-...",
        "Name": "Service 'person-service-client' check",
        "Status": "passing",
        "Output": "HTTP GET http://service-1:8081/actuator/health: 200 OK Output: {\"status\":\"UP\"}"
      }
    ]
  }
]
```

#### Via Consul UI
Navigate to http://localhost:8500/ui/dc1/services/person-service-client

Click on any instance to see:
- Current health status
- Last check time
- Check output
- Historical status changes

---

## Consul UI Walkthrough

### Accessing the UI

Open your browser and navigate to: **http://localhost:8500/ui**

You'll see the Consul UI dashboard showing the datacenter view (dc1 by default).

### Main Navigation

The left sidebar contains:

1. **Services** - View all registered services
2. **Nodes** - View Consul agent nodes
3. **Key/Value** - Access the distributed KV store
4. **Intentions** - Configure service mesh policies (Consul Connect)

### Services View

#### Services List

Click "Services" to see all registered services:

```
┌─────────────────────────────────────────────────────┐
│ Services (3)                              [+ Create]│
├─────────────────────────────────────────────────────┤
│ ✓ consul                                    1/1     │
│   Type: consul                                      │
│   Last registered: 2 hours ago                      │
├─────────────────────────────────────────────────────┤
│ ✓ person-front                              1/1     │
│   Type: http                                        │
│   Last registered: 1 hour ago                       │
├─────────────────────────────────────────────────────┤
│ ✓ person-service-client                     3/3     │
│   Type: http                                        │
│   Last registered: 1 hour ago                       │
└─────────────────────────────────────────────────────┘
```

**Status Indicators**:
- ✓ (green checkmark) = All instances healthy
- ⚠ (yellow warning) = Some instances unhealthy
- ✗ (red X) = All instances unhealthy

**Instance Count**: Shows "healthy/total" (e.g., "3/3" means 3 out of 3 instances are healthy)

#### Service Detail View

Click on "person-service-client" to see detailed information:

**Overview Tab**:
```
┌─────────────────────────────────────────────────────┐
│ person-service-client                               │
│ 3 instances across 1 datacenter                    │
├─────────────────────────────────────────────────────┤
│ Instances (3)                                       │
├─────────────────────────────────────────────────────┤
│ ✓ person-service-client-8081-951df...              │
│   Address: service-1:8081                           │
│   ID: person-service-client-8081-951df...           │
│   Tags: -                                           │
│   Health: passing                                   │
│   Last check: 3 seconds ago                         │
├─────────────────────────────────────────────────────┤
│ ✓ person-service-client-8082-a234e...              │
│   Address: service-2:8082                           │
│   ID: person-service-client-8082-a234e...           │
│   Tags: -                                           │
│   Health: passing                                   │
│   Last check: 5 seconds ago                         │
├─────────────────────────────────────────────────────┤
│ ✓ person-service-client-8083-b567f...              │
│   Address: service-3:8083                           │
│   ID: person-service-client-8083-b567f...           │
│   Tags: -                                           │
│   Health: passing                                   │
│   Last check: 2 seconds ago                         │
└─────────────────────────────────────────────────────┘
```

#### Instance Detail View

Click on any instance to see even more details:

**Health Checks Tab**:
```
┌─────────────────────────────────────────────────────┐
│ Health Checks (2)                                   │
├─────────────────────────────────────────────────────┤
│ ✓ Serf Health Status                               │
│   Type: serf                                        │
│   Status: passing                                   │
│   Output: Agent alive and reachable                │
│   Last check: 1 second ago                          │
├─────────────────────────────────────────────────────┤
│ ✓ Service 'person-service-client' check            │
│   Type: http                                        │
│   Status: passing                                   │
│   Output: HTTP GET http://service-1:8081/...       │
│           200 OK                                    │
│           Output: {"status":"UP"}                   │
│   Last check: 2 seconds ago                         │
│   Interval: 10s                                     │
│   Timeout: 5s                                       │
└─────────────────────────────────────────────────────┘
```

**Metadata Tab**:
Shows service metadata and tags.

### Nodes View

Click "Nodes" to see Consul agent nodes:

```
┌─────────────────────────────────────────────────────┐
│ Nodes (1)                                           │
├─────────────────────────────────────────────────────┤
│ ✓ docker-container                                 │
│   Address: 172.20.0.2                               │
│   Datacenter: dc1                                   │
│   Services: 4 (consul, person-front, person-...)   │
│   Health checks: 7 passing                          │
│   Leader: Yes                                       │
└─────────────────────────────────────────────────────┘
```

### Key/Value Store

Click "Key/Value" to access the distributed configuration store:

**Use Cases**:
- Feature flags
- Application configuration
- Dynamic property updates
- Coordination between services

**Example**: Store database connection pool size
```
config/person-service/db.pool.size = 20
```

Services can watch for changes and reload configuration without restart.

### Useful UI Features

#### Search & Filter
- Use the search box at the top to filter services
- Filter by health status (passing, warning, critical)
- Filter by datacenter

#### Refresh Interval
- UI auto-refreshes every 10 seconds
- Manual refresh button available
- Shows "Last updated" timestamp

#### Service Tags
- Services can have tags for categorization
- Filter services by tags
- Useful for environment separation (dev, staging, prod)

---

## Troubleshooting Consul Issues

### Issue 1: Service Not Appearing in Registry

**Symptoms**:
- Service starts successfully
- Consul UI shows no service instance
- No errors in application logs

**Possible Causes & Solutions**:

1. **Consul not running**
   ```bash
   # Check if Consul is running
   docker compose ps consul
   
   # Start Consul if stopped
   docker compose up -d consul
   ```

2. **Wrong Consul host configuration**
   ```yaml
   # Check application.yml
   spring:
     cloud:
       consul:
         host: consul  # Should match Docker service name or localhost
   ```

3. **Registration disabled**
   ```yaml
   spring:
     cloud:
       consul:
         discovery:
           enabled: true  # Must be true
           register: true  # Must be true
   ```

4. **Network connectivity issue**
   ```bash
   # Test from service container
   docker exec service-1 curl http://consul:8500/v1/agent/checks
   ```

### Issue 2: Health Checks Failing

**Symptoms**:
- Service appears in Consul but marked as "critical"
- Red X icon in Consul UI
- Service not receiving traffic

**Possible Causes & Solutions**:

1. **Actuator endpoint not accessible**
   ```bash
   # Test health endpoint directly
   curl http://localhost:8081/actuator/health
   
   # If 404, check Actuator dependency and configuration
   ```

2. **Wrong health check path**
   ```yaml
   spring:
     cloud:
       consul:
         discovery:
           health-check-path: /actuator/health  # Correct path
   ```

3. **Database connection failure**
   ```bash
   # Check database connectivity
   docker compose logs mysql
   docker exec service-1 curl http://localhost:8081/actuator/health
   ```

4. **Health check timeout**
   ```yaml
   spring:
     cloud:
       consul:
         discovery:
           health-check-timeout: 10s  # Increase if needed
   ```

### Issue 3: Service Discovery Not Working

**Symptoms**:
- Frontend can't connect to backend
- RestTemplate throws UnknownHostException
- "No instances available" error

**Possible Causes & Solutions**:

1. **@LoadBalanced annotation missing**
   ```groovy
   @Bean
   @LoadBalanced  // ← Must have this annotation
   RestTemplate restTemplate() {
       new RestTemplate()
   }
   ```

2. **Wrong service name in URL**
   ```groovy
   // Wrong - using IP/hostname
   def url = "http://service-1:8081/people"
   
   // Correct - using logical service name
   def url = "http://person-service-client/people"
   ```

3. **Load balancer not configured**
   ```xml
   <!-- Ensure this dependency is present -->
   <dependency>
       <groupId>org.springframework.cloud</groupId>
       <artifactId>spring-cloud-starter-loadbalancer</artifactId>
   </dependency>
   ```

4. **No healthy instances available**
   ```bash
   # Check if backend services are healthy
   curl http://localhost:8500/v1/health/service/person-service-client
   ```

### Issue 4: Connection Refused to Consul

**Symptoms**:
- "Connection refused" error on startup
- Service fails to start
- Cannot register with Consul

**Solutions**:

1. **Enable fail-fast=false for development**
   ```yaml
   spring:
     cloud:
       consul:
         discovery:
           fail-fast: false  # Don't fail startup if Consul unreachable
   ```

2. **Start Consul before services**
   ```bash
   # Correct order
   docker compose up -d consul
   sleep 5  # Wait for Consul to be ready
   docker compose up -d service-1 service-2 service-3
   ```

3. **Check network connectivity**
   ```bash
   # From host machine
   curl http://localhost:8500/v1/status/leader
   
   # From container
   docker exec service-1 ping consul
   ```

### Issue 5: Stale Service Instances

**Symptoms**:
- Stopped instances still appear in Consul
- Traffic sent to dead instances
- Errors after scaling down

**Solution**:

Wait for automatic deregistration (30 minutes by default) or manually deregister:

```bash
# Get service ID
curl http://localhost:8500/v1/catalog/service/person-service-client

# Deregister manually
curl -X PUT http://localhost:8500/v1/agent/service/deregister/person-service-client-8081-abc123
```

### Issue 6: DNS Resolution Failures

**Symptoms**:
- Cannot resolve service names via DNS
- dig/nslookup queries fail

**Solution**:

Configure Docker to use Consul DNS:

```yaml
services:
  service-1:
    dns:
      - 172.20.0.2  # Consul container IP
    dns_search:
      - service.consul
```

### Debug Logging

Enable debug logging for Consul integration:

```yaml
logging:
  level:
    org.springframework.cloud.consul: DEBUG
    com.ecwid.consul: DEBUG
```

---

## Best Practices

### 1. Service Naming Conventions

**Use descriptive, hierarchical names**:
```
Good:
  - user-service
  - order-service
  - payment-gateway

Bad:
  - service1
  - app
  - backend
```

**Include environment in tags (production)**:
```yaml
spring:
  cloud:
    consul:
      discovery:
        tags:
          - environment:production
          - version:1.2.3
          - region:us-west-2
```

### 2. Health Check Design

**Keep health checks fast** (< 1 second):
```groovy
@Component
class DatabaseHealthIndicator implements HealthIndicator {
    @Override
    Health health() {
        // Quick check - just validate connection
        if (dataSource.isValid(1)) {
            return Health.up().build()
        }
        return Health.down().build()
    }
}
```

**Don't include external dependencies in critical health checks**:
```groovy
// Bad - slow third-party API call in health check
Health health() {
    try {
        httpClient.get("https://slow-api.com/status")  // ← Avoid
        return Health.up().build()
    } catch (Exception e) {
        return Health.down().build()
    }
}

// Good - local resource check only
Health health() {
    if (cache.isAvailable()) {
        return Health.up().build()
    }
    return Health.down().build()
}
```

### 3. Graceful Shutdown

Ensure proper deregistration:

```yaml
spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s

management:
  endpoint:
    shutdown:
      enabled: true
```

```groovy
@Component
class GracefulShutdown {
    
    @PreDestroy
    void onShutdown() {
        log.info("Graceful shutdown initiated")
        // Wait for in-flight requests to complete
        Thread.sleep(5000)
        log.info("Shutdown complete")
    }
}
```

### 4. Instance ID Uniqueness

Always use unique instance IDs:

```yaml
# Good - includes random component
instance-id: ${spring.application.name}:${SERVER_PORT}:${random.value}

# Bad - not unique across restarts
instance-id: ${spring.application.name}:${SERVER_PORT}
```

### 5. Fail-Fast Configuration

**Development**: `fail-fast: false` (service can start without Consul)
**Production**: `fail-fast: true` (service must register or fail)

### 6. Monitoring Consul Health

**Set up alerts for**:
- Service deregistration events
- Health check failures
- Consul cluster issues
- High service churn rate

### 7. Service Metadata

Use metadata for additional context:

```yaml
spring:
  cloud:
    consul:
      discovery:
        metadata:
          version: ${project.version}
          git-commit: ${git.commit.id.abbrev}
          build-time: ${maven.build.timestamp}
          team: backend-team
```

Access metadata:

```groovy
@Autowired
DiscoveryClient discoveryClient

def getServiceMetadata() {
    List<ServiceInstance> instances = discoveryClient.getInstances("person-service-client")
    instances.each { instance ->
        log.info("Version: ${instance.metadata.version}")
        log.info("Git commit: ${instance.metadata['git-commit']}")
    }
}
```

### 8. Load Balancer Configuration

Configure timeouts and retries:

```yaml
spring:
  cloud:
    loadbalancer:
      retry:
        enabled: true
        max-attempts-on-next-service-instance: 2
        max-attempts-on-same-service-instance: 1
      configurations: default
      health-check:
        initial-delay: 0
        interval: 25s
```

---

## Advanced Topics

### Multi-Datacenter Setup

For production, run Consul clusters in multiple datacenters:

```yaml
consul:
  command: >
    agent -server -bootstrap-expect=3
    -ui -client=0.0.0.0
    -datacenter=us-west-2
    -join=consul-dc2:8301
```

**Cross-DC service discovery**:
```groovy
// Query service in different datacenter
def url = "http://person-service-client.service.us-east-1.consul/people"
```

### Consul Connect (Service Mesh)

Enable mutual TLS between services:

```yaml
spring:
  cloud:
    consul:
      discovery:
        register: true
      config:
        enabled: true
        prefix: config
        default-context: application
      connect:
        enabled: true
```

**Benefits**:
- Automatic mutual TLS
- Traffic encryption
- Service-to-service authorization
- Zero-trust security model

### Configuration Management

Store application configuration in Consul KV:

```yaml
spring:
  cloud:
    consul:
      config:
        enabled: true
        prefix: config
        default-context: application
        profile-separator: '::'
        format: YAML
        data-key: data
```

**Store config in Consul**:
```bash
# Put configuration
consul kv put config/person-service/data @config.yml

# Get configuration
consul kv get config/person-service/data
```

**Dynamic reload**:
```groovy
@RefreshScope
@RestController
class DynamicController {
    
    @Value('${feature.new-ui.enabled:false}')
    boolean newUiEnabled
    
    @GetMapping("/config")
    def getConfig() {
        [newUiEnabled: newUiEnabled]
    }
}
```

### ACLs and Security

Enable ACLs for production:

```bash
# Bootstrap ACL system
consul acl bootstrap

# Create service token
consul acl token create \
  -description "person-service-client token" \
  -service-identity "person-service-client"
```

```yaml
spring:
  cloud:
    consul:
      token: ${CONSUL_TOKEN}  # From environment variable
```

### Consul Watches

React to service changes:

```bash
# Watch for service changes
consul watch -type=service -service=person-service-client \
  ./notify-on-change.sh
```

```groovy
// In Spring Boot application
@Component
class ServiceChangeListener {
    
    @Autowired
    ConsulClient consulClient
    
    @Scheduled(fixedDelay = 10000)
    def watchServiceChanges() {
        List<ServiceNode> nodes = consulClient.getHealthServices(
            "person-service-client", 
            true, 
            null
        ).getValue()
        
        log.info("Current instances: ${nodes.size()}")
    }
}
```

---

## Summary

✅ **Consul provides**:
- Automatic service discovery
- Health checking and monitoring
- Load balancing across instances
- Key/Value configuration store
- Service mesh capabilities (Connect)

✅ **Key takeaways**:
- Use `@EnableDiscoveryClient` for registration
- Use `@LoadBalanced RestTemplate` for discovery
- Configure health checks via `/actuator/health`
- Monitor via Consul UI (http://localhost:8500/ui)
- Use logical service names, not IPs/hostnames

✅ **Production checklist**:
- [ ] Run multi-node Consul cluster (3-5 nodes)
- [ ] Enable fail-fast mode
- [ ] Configure appropriate health check intervals
- [ ] Set up monitoring and alerts
- [ ] Enable ACLs for security
- [ ] Use tags for environment separation
- [ ] Document service dependencies
- [ ] Test failover scenarios

---

## Additional Resources

- [Consul Documentation](https://www.consul.io/docs)
- [Spring Cloud Consul](https://spring.io/projects/spring-cloud-consul)
- [Consul UI Guide](https://www.consul.io/docs/ui)
- [Service Discovery Patterns](https://microservices.io/patterns/service-registry.html)
- [Project README](../README.md)
- [Docker Commands Reference](docker-commands.md)

---

**Last Updated**: April 20, 2026  
**Version**: 1.0  
**Feedback**: Please report issues or suggest improvements via GitHub issues.
