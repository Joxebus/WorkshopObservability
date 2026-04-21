# Metrics & Monitoring Guide

**Version**: 1.0  
**Last Updated**: April 20, 2026  
**Target Audience**: Developers, DevOps Engineers, SREs

---

## Table of Contents

1. [Introduction to Application Metrics](#1-introduction-to-application-metrics)
2. [Spring Boot Actuator Metrics](#2-spring-boot-actuator-metrics)
3. [Custom Application Metrics](#3-custom-application-metrics)
4. [Consul Health Metrics](#4-consul-health-metrics)
5. [Database Performance Metrics](#5-database-performance-metrics)
6. [Elasticsearch Cluster Metrics](#6-elasticsearch-cluster-metrics)
7. [Interpreting Metric Trends](#7-interpreting-metric-trends)
8. [Setting Up Dashboards](#8-setting-up-dashboards)
9. [Alerting Strategy](#9-alerting-strategy)
10. [Metric Retention & Storage](#10-metric-retention--storage)
11. [Integrating with External Monitoring](#11-integrating-with-external-monitoring)
12. [Performance Baseline Establishment](#12-performance-baseline-establishment)

---

## 1. Introduction to Application Metrics

### What are Application Metrics?

Application metrics are **quantitative measurements** that provide insights into the behavior, performance, and health of your application. Unlike logs (which are event-based records) and traces (which track request flows), metrics aggregate numerical data over time.

### The Three Pillars of Observability

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│    LOGS     │     │   METRICS   │     │   TRACES    │
│             │     │             │     │             │
│ What        │     │ How much/   │     │ Where &     │
│ happened?   │     │ how many?   │     │ how long?   │
└─────────────┘     └─────────────┘     └─────────────┘
      ↓                    ↓                    ↓
  Event-based        Time-series          Request flow
  Detailed           Aggregated           Distributed
  Searchable         Efficient            End-to-end
```

### Types of Metrics

#### Counter
- **Purpose**: Track cumulative values that only increase
- **Examples**: 
  - Total HTTP requests served
  - Number of persons created
  - Error count
- **When to use**: When you want to count events or operations

```groovy
Counter requestCounter = meterRegistry.counter("http.requests.total")
requestCounter.increment()  // Increment by 1
```

#### Gauge
- **Purpose**: Track values that can go up or down
- **Examples**:
  - Current memory usage
  - Active database connections
  - Queue size
  - Current temperature
- **When to use**: When measuring current state or level

```groovy
meterRegistry.gauge("database.connections.active", connectionPool, 
    pool -> pool.getActiveConnections())
```

#### Histogram
- **Purpose**: Track the distribution of values
- **Examples**:
  - Request duration distribution
  - Response payload sizes
- **When to use**: When you need percentiles (p50, p95, p99)

```groovy
Timer timer = meterRegistry.timer("http.request.duration")
timer.record(() -> {
    // Execute operation
})
```

#### Summary
- **Purpose**: Similar to histogram but calculates quantiles on client side
- **Examples**:
  - Response time summaries
- **When to use**: When you need flexible quantile calculation

### Why Metrics Matter for Microservices

In a microservices architecture like ours (frontend + 3 backend instances + infrastructure), metrics are essential because:

1. **Distributed System Visibility**: Track behavior across multiple service instances
2. **Performance Monitoring**: Identify bottlenecks and slow operations
3. **Capacity Planning**: Understand resource usage trends
4. **Alerting**: Detect anomalies before they become incidents
5. **SLA Compliance**: Measure and prove service level objectives
6. **Cost Optimization**: Identify underutilized or overprovisioned resources

---

## 2. Spring Boot Actuator Metrics

### Enabling Actuator

Our application already has Spring Boot Actuator enabled. The configuration is in `application.yml`:

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

### Available Actuator Endpoints

#### Health Endpoint
```bash
# Check service health
curl http://localhost:8081/actuator/health

# Response
{
  "status": "UP",
  "components": {
    "consul": {
      "status": "UP",
      "details": {
        "leader": "127.0.0.1:8300",
        "services": {
          "person-service-client": 3,
          "person-front": 1
        }
      }
    },
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
        "total": 500107862016,
        "free": 250000000000,
        "threshold": 10485760
      }
    }
  }
}
```

#### Metrics List Endpoint
```bash
# Get all available metrics
curl http://localhost:8081/actuator/metrics

# Response
{
  "names": [
    "jvm.memory.used",
    "jvm.memory.max",
    "jvm.gc.pause",
    "http.server.requests",
    "hikaricp.connections.active",
    "system.cpu.usage",
    "process.uptime",
    ...
  ]
}
```

#### Specific Metric Endpoint
```bash
# Get specific metric details
curl http://localhost:8081/actuator/metrics/jvm.memory.used

# Response
{
  "name": "jvm.memory.used",
  "description": "The amount of used memory",
  "baseUnit": "bytes",
  "measurements": [
    {
      "statistic": "VALUE",
      "value": 268435456
    }
  ],
  "availableTags": [
    {
      "tag": "area",
      "values": ["heap", "nonheap"]
    },
    {
      "tag": "id",
      "values": ["G1 Eden Space", "G1 Old Gen", "Metaspace"]
    }
  ]
}
```

### Key Metrics Categories

#### JVM Metrics

**Memory Usage**:
```bash
# Heap memory used
curl http://localhost:8081/actuator/metrics/jvm.memory.used?tag=area:heap

# Heap memory max
curl http://localhost:8081/actuator/metrics/jvm.memory.max?tag=area:heap

# Memory committed (guaranteed available)
curl http://localhost:8081/actuator/metrics/jvm.memory.committed?tag=area:heap
```

**Typical values for our application**:
- Heap Used: 100-400 MB (varies with load)
- Heap Max: 512 MB (Docker container limit)
- Heap Committed: 256-512 MB

**Garbage Collection**:
```bash
# GC pause time
curl http://localhost:8081/actuator/metrics/jvm.gc.pause

# GC count
curl http://localhost:8081/actuator/metrics/jvm.gc.count
```

**Expected GC behavior**:
- **Young GC**: Frequent (every few seconds), fast (< 50ms)
- **Old GC**: Infrequent (minutes to hours), slower (100-500ms)
- **Warning signs**: Old GC > 1 second, frequent full GCs

**Thread Metrics**:
```bash
# Live threads
curl http://localhost:8081/actuator/metrics/jvm.threads.live

# Daemon threads
curl http://localhost:8081/actuator/metrics/jvm.threads.daemon

# Peak threads
curl http://localhost:8081/actuator/metrics/jvm.threads.peak
```

**Normal thread counts**:
- Live threads: 20-50 (depends on load)
- Daemon threads: 15-30
- Peak threads: Should stabilize after warmup

#### HTTP Server Metrics

**Request Count and Duration**:
```bash
# All HTTP requests
curl http://localhost:8081/actuator/metrics/http.server.requests

# Filter by URI
curl 'http://localhost:8081/actuator/metrics/http.server.requests?tag=uri:/people'

# Filter by status code
curl 'http://localhost:8081/actuator/metrics/http.server.requests?tag=status:200'

# Filter by HTTP method
curl 'http://localhost:8081/actuator/metrics/http.server.requests?tag=method:GET'
```

**Response format**:
```json
{
  "name": "http.server.requests",
  "measurements": [
    {
      "statistic": "COUNT",
      "value": 1523
    },
    {
      "statistic": "TOTAL_TIME",
      "value": 45.678
    },
    {
      "statistic": "MAX",
      "value": 0.523
    }
  ],
  "availableTags": [
    {"tag": "exception", "values": ["None", "NullPointerException"]},
    {"tag": "method", "values": ["GET", "POST", "PUT", "DELETE"]},
    {"tag": "uri", "values": ["/people", "/people/{id}"]},
    {"tag": "status", "values": ["200", "404", "500"]}
  ]
}
```

**Interpreting HTTP metrics**:
- **COUNT**: Total requests served (should always increase)
- **TOTAL_TIME**: Sum of all request durations in seconds
- **MAX**: Slowest request in the time window
- **Average response time**: TOTAL_TIME / COUNT

**Performance targets**:
- **p50 (median)**: < 50ms for simple queries
- **p95**: < 200ms
- **p99**: < 500ms
- **Max**: < 2 seconds (unless expected for specific operations)

#### Data Source Metrics (HikariCP)

HikariCP is the default connection pool in Spring Boot. It provides excellent metrics:

```bash
# Active connections
curl http://localhost:8081/actuator/metrics/hikaricp.connections.active

# Idle connections
curl http://localhost:8081/actuator/metrics/hikaricp.connections.idle

# Total connections
curl http://localhost:8081/actuator/metrics/hikaricp.connections

# Pending threads (waiting for connection)
curl http://localhost:8081/actuator/metrics/hikaricp.connections.pending

# Connection timeout
curl http://localhost:8081/actuator/metrics/hikaricp.connections.timeout.total

# Connection creation time
curl http://localhost:8081/actuator/metrics/hikaricp.connections.creation

# Connection acquire time
curl http://localhost:8081/actuator/metrics/hikaricp.connections.acquire
```

**Healthy connection pool indicators**:
- **Active**: 0-5 (most of the time)
- **Idle**: 5-10 (ready for burst traffic)
- **Pending**: 0 (no threads waiting)
- **Timeout**: 0 (no connection acquisition failures)
- **Acquire time**: < 10ms

**Warning signs**:
- Active = Max pool size (exhausted)
- Pending > 0 (threads waiting)
- Timeouts increasing
- Acquire time > 100ms

#### Tomcat Metrics

**Thread Pool**:
```bash
# Busy threads (processing requests)
curl http://localhost:8081/actuator/metrics/tomcat.threads.busy

# Current threads
curl http://localhost:8081/actuator/metrics/tomcat.threads.current

# Max threads configured
curl http://localhost:8081/actuator/metrics/tomcat.threads.config.max
```

**Typical values**:
- Busy: 1-10 under normal load
- Current: 10-25 (auto-scales)
- Max: 200 (default)

**Session Metrics**:
```bash
# Active sessions
curl http://localhost:8081/actuator/metrics/tomcat.sessions.active.current

# Maximum active sessions
curl http://localhost:8081/actuator/metrics/tomcat.sessions.active.max

# Sessions created
curl http://localhost:8081/actuator/metrics/tomcat.sessions.created

# Sessions rejected
curl http://localhost:8081/actuator/metrics/tomcat.sessions.rejected
```

---

## 3. Custom Application Metrics

### Using Micrometer

Micrometer is the metrics facade used by Spring Boot Actuator. Think of it as SLF4J but for metrics.

**Dependency** (already included via `spring-boot-starter-actuator`):
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-core</artifactId>
</dependency>
```

### Counter Example: Track Person Operations

Add custom metrics to `PersonServiceImpl`:

```groovy
package io.github.joxebus.service.impl

import io.github.joxebus.domain.Person
import io.github.joxebus.repository.PersonRepository
import io.github.joxebus.service.PersonService
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import jakarta.annotation.PostConstruct
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class PersonServiceImpl implements PersonService {

    @Autowired
    PersonRepository personRepository

    @Autowired
    Validator validator

    @Autowired
    MeterRegistry meterRegistry

    private Counter personCreatedCounter
    private Counter personDeletedCounter
    private Counter personUpdatedCounter
    private Counter personValidationErrorCounter

    @PostConstruct
    void init() {
        // Initialize counters
        personCreatedCounter = meterRegistry.counter("person.created", "result", "success")
        personDeletedCounter = meterRegistry.counter("person.deleted", "result", "success")
        personUpdatedCounter = meterRegistry.counter("person.updated", "result", "success")
        personValidationErrorCounter = meterRegistry.counter("person.validation.error")
    }

    @Override
    Person save(Person person) {
        def violations = validator.validate(person)
        if (!violations.empty) {
            personValidationErrorCounter.increment()
            throw new ConstraintViolationException("Person fields are incorrect", violations)
        }

        Person savedPerson = personRepository.save(person)
        
        // Track if this is create or update
        if (person.id == null) {
            personCreatedCounter.increment()
        } else {
            personUpdatedCounter.increment()
        }
        
        return savedPerson
    }

    @Override
    boolean delete(Long id) {
        Person person = findById(id)
        if (person) {
            personRepository.delete(person)
            personDeletedCounter.increment()
            return true
        }
        return false
    }

    // ... other methods
}
```

**Query the custom metrics**:
```bash
# Person created count
curl http://localhost:8081/actuator/metrics/person.created

# Person deleted count
curl http://localhost:8081/actuator/metrics/person.deleted

# Validation errors
curl http://localhost:8081/actuator/metrics/person.validation.error
```

### Gauge Example: Track Repository Size

Add a gauge for the current person count:

```groovy
@PostConstruct
void init() {
    // ... counters from above
    
    // Gauge that tracks repository size
    meterRegistry.gauge("person.repository.count", personRepository, 
        repo -> repo.count().doubleValue())
}
```

**Query the gauge**:
```bash
curl http://localhost:8081/actuator/metrics/person.repository.count
```

This gauge will automatically reflect the current count whenever queried.

### Timer Example: Measure Operation Duration

Track how long save operations take:

```groovy
@Override
Person save(Person person) {
    return meterRegistry.timer("person.save.duration").recordCallable(() -> {
        def violations = validator.validate(person)
        if (!violations.empty) {
            personValidationErrorCounter.increment()
            throw new ConstraintViolationException("Person fields are incorrect", violations)
        }

        Person savedPerson = personRepository.save(person)
        
        if (person.id == null) {
            personCreatedCounter.increment()
        } else {
            personUpdatedCounter.increment()
        }
        
        return savedPerson
    })
}
```

**Query the timer**:
```bash
curl http://localhost:8081/actuator/metrics/person.save.duration

# Response includes percentiles
{
  "name": "person.save.duration",
  "measurements": [
    {"statistic": "COUNT", "value": 150},
    {"statistic": "TOTAL_TIME", "value": 7.5},
    {"statistic": "MAX", "value": 0.234}
  ]
}
```

### Custom Tags for Segmentation

Add tags to metrics for better filtering:

```groovy
personCreatedCounter = meterRegistry.counter("person.operations", 
    "operation", "create",
    "result", "success")

personCreateFailedCounter = meterRegistry.counter("person.operations",
    "operation", "create", 
    "result", "failed")
```

Now you can query:
```bash
# All person operations
curl http://localhost:8081/actuator/metrics/person.operations

# Only create operations
curl 'http://localhost:8081/actuator/metrics/person.operations?tag=operation:create'

# Only failed operations
curl 'http://localhost:8081/actuator/metrics/person.operations?tag=result:failed'
```

---

## 4. Consul Health Metrics

### Service Registry Metrics

#### Using Consul HTTP API

**Get all registered services**:
```bash
curl http://localhost:8500/v1/catalog/services | jq .

# Response
{
  "consul": [],
  "person-front": [],
  "person-service-client": []
}
```

**Get service instances**:
```bash
curl http://localhost:8500/v1/catalog/service/person-service-client | jq .

# Response
[
  {
    "ID": "service-1",
    "Node": "consul-node",
    "Address": "172.18.0.5",
    "ServiceID": "person-service-client:8081:12345",
    "ServiceName": "person-service-client",
    "ServicePort": 8081,
    "ServiceMeta": {}
  },
  {
    "ID": "service-2",
    "Node": "consul-node",
    "ServicePort": 8082
  },
  {
    "ID": "service-3",
    "Node": "consul-node",
    "ServicePort": 8083
  }
]
```

**Count service instances**:
```bash
# Count backend service instances
curl -s http://localhost:8500/v1/catalog/service/person-service-client | jq 'length'

# Expected: 3
```

### Health Check Metrics

**Get health status for all instances**:
```bash
curl http://localhost:8500/v1/health/service/person-service-client | jq .

# Response includes health checks
[
  {
    "Node": {...},
    "Service": {
      "ID": "person-service-client:8081:12345",
      "Service": "person-service-client",
      "Port": 8081
    },
    "Checks": [
      {
        "Node": "consul-node",
        "CheckID": "serfHealth",
        "Name": "Serf Health Status",
        "Status": "passing",
        "Output": "Agent alive and reachable"
      },
      {
        "CheckID": "service:person-service-client:8081:12345",
        "Name": "Service 'person-service-client' check",
        "Status": "passing",
        "Output": "HTTP GET http://service-1:8081/actuator/health: 200 OK"
      }
    ]
  }
]
```

**Filter by health status**:
```bash
# Only healthy instances
curl http://localhost:8500/v1/health/service/person-service-client?passing | jq 'length'

# All instances (including unhealthy)
curl http://localhost:8500/v1/health/service/person-service-client | jq 'length'

# Calculate health percentage
TOTAL=$(curl -s http://localhost:8500/v1/health/service/person-service-client | jq 'length')
HEALTHY=$(curl -s http://localhost:8500/v1/health/service/person-service-client?passing | jq 'length')
echo "Health: $HEALTHY/$TOTAL ($(( HEALTHY * 100 / TOTAL ))%)"
```

### Monitoring Consul Agent

**Consul agent metrics** (exposed in Prometheus format):
```bash
curl http://localhost:8500/v1/agent/metrics

# Response includes
{
  "Timestamp": "2026-04-20T10:00:00Z",
  "Gauges": [
    {"Name": "consul.runtime.alloc_bytes", "Value": 50000000},
    {"Name": "consul.runtime.num_goroutines", "Value": 150},
    {"Name": "consul.serf.member.flap", "Value": 0}
  ],
  "Counters": [
    {"Name": "consul.catalog.register", "Count": 3},
    {"Name": "consul.catalog.deregister", "Count": 0}
  ]
}
```

**Key Consul metrics to monitor**:
- `consul.runtime.alloc_bytes` - Memory usage
- `consul.runtime.num_goroutines` - Concurrent operations
- `consul.catalog.service.register` - Services joining
- `consul.catalog.service.deregister` - Services leaving
- `consul.health.service.query` - Health check queries

---

## 5. Database Performance Metrics

### MySQL Performance Metrics

#### Using MySQL Commands

**Connect to MySQL**:
```bash
# From host machine
docker exec -it mysql mysql -u consul -pexample consul-example

# Or via Docker Compose
docker compose exec mysql mysql -u consul -pexample consul-example
```

#### Connection Metrics

```sql
-- Current connections
SHOW STATUS LIKE 'Threads_connected';
-- Shows: number of currently open connections

-- Peak connections since startup
SHOW STATUS LIKE 'Max_used_connections';

-- Connection limit
SHOW VARIABLES LIKE 'max_connections';

-- Aborted connections (failures)
SHOW STATUS LIKE 'Aborted_connects';
```

**Interpreting results**:
```
Threads_connected: 5
Max_used_connections: 8
max_connections: 151
Aborted_connects: 0
```

- **Healthy**: Connected < 20% of max_connections
- **Warning**: Connected > 50% of max_connections
- **Critical**: Connected > 80% of max_connections
- **Aborted > 0**: Check authentication issues, network problems

#### Query Performance

```sql
-- Slow queries count
SHOW STATUS LIKE 'Slow_queries';

-- Total queries executed
SHOW STATUS LIKE 'Questions';

-- Queries per second (approximate)
-- Run twice with 1 second gap, calculate difference
SHOW GLOBAL STATUS LIKE 'Questions';
```

**Slow query log configuration**:
```sql
-- Check if slow query log is enabled
SHOW VARIABLES LIKE 'slow_query_log';

-- Slow query threshold
SHOW VARIABLES LIKE 'long_query_time';

-- Enable slow query log (if needed)
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 2;  -- 2 seconds
```

#### Table Statistics

```sql
-- Table sizes
SELECT 
    table_name,
    ROUND(((data_length + index_length) / 1024 / 1024), 2) AS "Size (MB)",
    table_rows
FROM information_schema.TABLES
WHERE table_schema = 'consul-example'
ORDER BY (data_length + index_length) DESC;

-- Expected output:
-- person | 0.05 MB | 150 rows
```

#### Query Execution Analysis

```sql
-- View currently running queries
SHOW FULL PROCESSLIST;

-- Response shows:
-- | Id | User   | Host           | db             | Command | Time | State       | Info                   |
-- | 10 | consul | 172.18.0.5:123 | consul-example | Query   | 0    | executing   | SELECT * FROM person   |
-- | 11 | consul | 172.18.0.6:456 | consul-example | Sleep   | 30   | -           | NULL                   |
```

**What to look for**:
- **Long Time values**: Queries taking > 2 seconds
- **Locked state**: Waiting for table locks (deadlock)
- **Many Sleep connections**: Connection pool not closing connections

#### Index Usage

```sql
-- Check if queries are using indexes
SHOW STATUS LIKE 'Handler_read%';

-- Handler_read_first: Full index scan
-- Handler_read_key: Reads using index
-- Handler_read_rnd_next: Reads without index (BAD!)

-- Analyze specific query
EXPLAIN SELECT * FROM person WHERE email = 'test@example.com';

-- Response should show:
-- type: ref (good) or ALL (bad - full table scan)
-- key: email_idx (using index)
```

### HikariCP Monitoring (from Application)

We covered HikariCP metrics in Section 2, but here's a monitoring script:

```bash
#!/bin/bash
# hikari-monitor.sh - Monitor connection pool health

SERVICE="localhost:8081"

echo "=== HikariCP Connection Pool Monitor ==="
echo "Service: $SERVICE"
echo ""

# Active connections
ACTIVE=$(curl -s "http://$SERVICE/actuator/metrics/hikaricp.connections.active" | jq '.measurements[0].value')
echo "Active connections: $ACTIVE"

# Idle connections
IDLE=$(curl -s "http://$SERVICE/actuator/metrics/hikaricp.connections.idle" | jq '.measurements[0].value')
echo "Idle connections: $IDLE"

# Total connections
TOTAL=$(curl -s "http://$SERVICE/actuator/metrics/hikaricp.connections" | jq '.measurements[0].value')
echo "Total connections: $TOTAL"

# Pending threads
PENDING=$(curl -s "http://$SERVICE/actuator/metrics/hikaricp.connections.pending" | jq '.measurements[0].value // 0')
echo "Pending threads: $PENDING"

# Health assessment
echo ""
if [ "$PENDING" -gt 0 ]; then
    echo "❌ WARNING: Threads are waiting for connections!"
elif [ "$ACTIVE" -eq "$TOTAL" ]; then
    echo "⚠️  WARNING: All connections in use (pool exhausted)"
else
    echo "✅ Connection pool healthy"
fi
```

---

## 6. Elasticsearch Cluster Metrics

### Cluster Health

```bash
# Overall cluster health
curl http://localhost:9200/_cluster/health?pretty

# Response
{
  "cluster_name": "docker-cluster",
  "status": "yellow",
  "timed_out": false,
  "number_of_nodes": 1,
  "number_of_data_nodes": 1,
  "active_primary_shards": 5,
  "active_shards": 5,
  "relocating_shards": 0,
  "initializing_shards": 0,
  "unassigned_shards": 5,
  "delayed_unassigned_shards": 0,
  "number_of_pending_tasks": 0,
  "number_of_in_flight_fetch": 0,
  "task_max_waiting_in_queue_millis": 0,
  "active_shards_percent_as_number": 50.0
}
```

**Status meanings**:
- **green**: All primary and replica shards are assigned
- **yellow**: All primary shards assigned, but some replicas are not (common in single-node setup)
- **red**: Some primary shards are unassigned (DATA LOSS RISK)

**Expected for our single-node setup**: `yellow` (because replicas can't be assigned to the same node as primaries)

### Index Statistics

```bash
# List all indices
curl http://localhost:9200/_cat/indices?v

# Response
health status index                     pri rep docs.count docs.deleted store.size pri.store.size
yellow open   logstash-2026.04.20       1   1      15234            0      4.2mb          4.2mb
yellow open   logstash-2026.04.19       1   1      28451            0      8.1mb          8.1mb
```

**Key columns**:
- **pri**: Number of primary shards
- **rep**: Number of replicas
- **docs.count**: Total documents in index
- **store.size**: Total storage (primary + replicas)
- **pri.store.size**: Primary shard storage only

### Node Statistics

```bash
# Node-level stats
curl http://localhost:9200/_nodes/stats?pretty

# Relevant metrics
{
  "nodes": {
    "node-id": {
      "jvm": {
        "mem": {
          "heap_used_in_bytes": 524288000,
          "heap_max_in_bytes": 1073741824
        },
        "gc": {
          "collectors": {
            "young": {
              "collection_count": 150,
              "collection_time_in_millis": 2500
            },
            "old": {
              "collection_count": 2,
              "collection_time_in_millis": 500
            }
          }
        }
      },
      "indices": {
        "docs": {
          "count": 43685,
          "deleted": 0
        },
        "store": {
          "size_in_bytes": 12500000
        },
        "indexing": {
          "index_total": 43685,
          "index_time_in_millis": 15000,
          "index_current": 0
        },
        "search": {
          "query_total": 450,
          "query_time_in_millis": 8500,
          "query_current": 1
        }
      }
    }
  }
}
```

**Key metrics to monitor**:
- **heap_used / heap_max**: Should stay below 75%
- **gc collection_count**: Frequent young GC is normal, old GC should be rare
- **indexing.index_current**: Active indexing operations (should be low)
- **search.query_current**: Active search queries

### Index Performance

```bash
# Indexing rate and search rate
curl http://localhost:9200/_stats/indexing,search?pretty

# Calculate rates (docs/second, queries/second)
# Run twice with time gap and calculate difference
```

**Healthy performance targets**:
- **Indexing latency**: < 100ms per document
- **Search latency**: < 50ms for simple queries
- **Refresh time**: 1-5 seconds (default is 1s)

### Monitoring Script

```bash
#!/bin/bash
# elasticsearch-monitor.sh

ES_HOST="localhost:9200"

echo "=== Elasticsearch Cluster Monitor ==="
echo ""

# Cluster health
STATUS=$(curl -s "$ES_HOST/_cluster/health" | jq -r '.status')
NODES=$(curl -s "$ES_HOST/_cluster/health" | jq '.number_of_nodes')
SHARDS=$(curl -s "$ES_HOST/_cluster/health" | jq '.active_shards')

echo "Cluster Status: $STATUS"
echo "Nodes: $NODES"
echo "Active Shards: $SHARDS"
echo ""

# Indices
echo "=== Recent Indices ==="
curl -s "$ES_HOST/_cat/indices/logstash-*?v&s=index:desc&h=index,docs.count,store.size" | head -n 5
echo ""

# JVM Heap
HEAP_USED=$(curl -s "$ES_HOST/_nodes/stats/jvm" | jq '.nodes | to_entries[0].value.jvm.mem.heap_used_percent')
echo "JVM Heap Usage: ${HEAP_USED}%"
echo ""

# Assessment
if [ "$STATUS" = "green" ]; then
    echo "✅ Cluster healthy"
elif [ "$STATUS" = "yellow" ]; then
    echo "⚠️  Cluster yellow (expected for single-node)"
else
    echo "❌ Cluster RED - immediate action required!"
fi
```

---

## 7. Interpreting Metric Trends

Understanding metrics is not just about reading numbers — it's about recognizing patterns and identifying anomalies.

### Memory Usage Patterns

#### Normal Pattern: Sawtooth

```
Memory Usage Over Time
  ^
  |     /\      /\      /\
  | ___/  \____/  \____/  \____
  |--------------------------------> Time
    GC    GC    GC
```

**What this means**:
- Memory grows as application allocates objects
- GC runs, freeing unused objects (sharp drop)
- Pattern repeats regularly
- **This is healthy!**

#### Abnormal Pattern: Memory Leak

```
Memory Usage Over Time
  ^
  |                        /\____
  |                /\____/
  |        /\____/
  |  _____/
  |--------------------------------> Time
    Never fully drops after GC
```

**What this means**:
- Memory keeps increasing despite GC
- Eventually leads to OutOfMemoryError
- **Action**: Heap dump analysis, find leaked objects

**Diagnose**:
```bash
# Get JVM memory metrics over time
watch -n 5 'curl -s http://localhost:8081/actuator/metrics/jvm.memory.used?tag=area:heap | jq .measurements[0].value'

# If value keeps increasing and never drops significantly -> memory leak
```

#### Abnormal Pattern: Undersized Heap

```
Memory Usage Over Time
  ^
Max|████████████████████████████████
  |    Constant GC pressure
  |--------------------------------> Time
    Always at or near max
```

**What this means**:
- Application needs more memory than available
- Continuous GC trying to free space
- Poor performance
- **Action**: Increase heap size

**Fix in Dockerfile**:
```dockerfile
CMD java -Xms512m -Xmx1024m ${JAVA_OPTS} -jar person-service.jar
```

### Response Time Patterns

#### Normal Pattern: Consistent Low Latency

```
Response Time (ms)
  ^
200|           *
100|  *  * *    *  *
 50|* * * * ** * * * *
  0|--------------------------------> Time
    Occasional spikes acceptable
```

**What this means**:
- Most requests fast (< 50ms)
- Occasional spikes due to GC, disk I/O, etc.
- **This is expected**

#### Abnormal Pattern: Gradual Increase

```
Response Time (ms)
  ^
500|                     * * *
300|              * * *
100|      * * *
 50|* * *
  0|--------------------------------> Time
    Performance degradation
```

**What this means**:
- Resource exhaustion (memory, connections, disk)
- Memory leak affecting performance
- Database query performance degrading
- **Action**: Investigate recent changes, check resource usage

**Diagnose**:
```bash
# Check HTTP request duration trend
curl 'http://localhost:8081/actuator/metrics/http.server.requests?tag=uri:/people' | jq .

# Check database connection pool
curl http://localhost:8081/actuator/metrics/hikaricp.connections.active | jq .

# Check JVM memory
curl 'http://localhost:8081/actuator/metrics/jvm.memory.used?tag=area:heap' | jq .
```

#### Abnormal Pattern: Bimodal Distribution

```
Response Time Distribution
  ^
  |  *              *
  |  *              *
  |  *              *
  |--------------------------------> Time
    Fast   vs    Slow
    50ms        500ms
```

**What this means**:
- Two distinct performance profiles
- Likely: cache hit (fast) vs cache miss (slow)
- Or: simple query (fast) vs complex query (slow)
- **Action**: Optimize slow path, improve caching

### Error Rate Patterns

#### Normal Pattern: Near Zero

```
Errors/sec
  ^
 10|
  5|
  1| *      *
  0|--------------------------------> Time
    Rare, isolated errors
```

**What this means**:
- Occasional errors expected (network issues, invalid input)
- No pattern or correlation
- **This is normal**

#### Abnormal Pattern: Sudden Spike

```
Errors/sec
  ^
100|        *
 50|       ***
 10|      *****
  0|___********_________________> Time
         Spike
```

**What this means**:
- Deployment issue (bad code)
- External dependency failure (database, Consul, other service)
- Configuration change
- **Action**: Rollback, check logs, verify dependencies

**Diagnose**:
```bash
# Check error count by status code
curl 'http://localhost:8081/actuator/metrics/http.server.requests?tag=status:500' | jq .

# Check recent logs in Kibana
# Go to Kibana Discover
# Filter: level:ERROR AND @timestamp:[now-15m TO now]

# Check Consul service health
curl http://localhost:8500/v1/health/service/person-service-client?passing | jq 'length'

# Check database connectivity
docker compose exec mysql mysqladmin ping
```

#### Abnormal Pattern: Gradual Increase

```
Errors/sec
  ^
 50|                     * * *
 30|              * * *
 10|      * * *
  1|* * *
  0|--------------------------------> Time
    Error rate climbing
```

**What this means**:
- Resource exhaustion approaching
- Database connection pool running out
- Memory leak causing instability
- **Action**: Check resource metrics, scale if needed

### Connection Pool Patterns

#### Normal Pattern: Mostly Idle

```
Connections
  ^
 10|************ Idle **************
  5|**** Active ****
  0|--------------------------------> Time
    Most connections idle
```

**What this means**:
- Pool has capacity for burst traffic
- Connections ready to use
- **Healthy**

#### Abnormal Pattern: Pool Exhausted

```
Connections
  ^
 10|********** All Active **********
  0|--------------------------------> Time
    Pool maxed out
```

**What this means**:
- All connections in use
- New requests wait or fail
- **Action**: Increase pool size or investigate slow queries

**Fix in `application-docker.yml`**:
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20  # Increase from default 10
      minimum-idle: 5
      connection-timeout: 30000
```

---

## 8. Setting Up Dashboards

### Dashboard Design Principles

1. **Top-to-Bottom Flow**: Most critical metrics at the top
2. **Logical Grouping**: Group related metrics together
3. **Consistent Time Range**: All panels use the same time range
4. **Clear Labels**: No jargon, explain what the metric means
5. **Color Coding**: Green (good), Yellow (warning), Red (critical)

### Dashboard 1: Service Overview

**Purpose**: Monitor the health and performance of our Spring Boot services

**Layout** (4 rows):

#### Row 1: Health Status (Single Stat Panels)
- **Service Instance Count**: `count(up{job="person-service"})`
  - Expected: 3
  - Red if < 3
- **Error Rate (%)**: `(sum(rate(http_server_requests_total{status=~"5.."}[5m])) / sum(rate(http_server_requests_total[5m]))) * 100`
  - Green: < 1%
  - Yellow: 1-5%
  - Red: > 5%
- **Average Response Time**: `avg(http_server_requests_seconds{uri="/people"})`
  - Green: < 100ms
  - Yellow: 100-500ms
  - Red: > 500ms

#### Row 2: Request Metrics (Time Series Graphs)
- **Request Rate (req/sec)**: Line graph showing `rate(http_server_requests_total[1m])`
  - Split by service instance
- **Response Time Percentiles**: Line graph
  - p50: `histogram_quantile(0.50, http_server_requests_seconds_bucket)`
  - p95: `histogram_quantile(0.95, http_server_requests_seconds_bucket)`
  - p99: `histogram_quantile(0.99, http_server_requests_seconds_bucket)`

#### Row 3: JVM Metrics (Mixed)
- **Heap Memory Usage**: Area graph
  - Used: `jvm_memory_used_bytes{area="heap"}`
  - Max: `jvm_memory_max_bytes{area="heap"}`
  - Yellow at 70%, Red at 85%
- **GC Pause Time**: Bar graph showing `rate(jvm_gc_pause_seconds_sum[1m])`
- **Thread Count**: Line graph of `jvm_threads_live`

#### Row 4: Database & External Dependencies
- **Active DB Connections**: Gauge showing `hikaricp_connections_active`
- **DB Connection Pool Usage (%)**: `(hikaricp_connections_active / hikaricp_connections_max) * 100`
- **Consul Registered Services**: Single stat from Consul API

**Creating in Grafana** (if integrated):
```json
{
  "dashboard": {
    "title": "Person Service Overview",
    "panels": [
      {
        "title": "Service Instances",
        "type": "singlestat",
        "targets": [
          {
            "expr": "count(up{job=\"person-service\"})"
          }
        ],
        "thresholds": "2,3",
        "colors": ["red", "yellow", "green"]
      }
    ]
  }
}
```

### Dashboard 2: Infrastructure Overview

**Purpose**: Monitor the health of supporting infrastructure (Consul, MySQL, Elasticsearch)

#### Row 1: Consul Metrics
- **Registered Services**: Count from `/v1/catalog/services`
- **Healthy Service Instances**: From `/v1/health/state/passing`
- **Health Check Failures**: From `/v1/health/state/critical`

#### Row 2: MySQL Metrics
- **Connection Count**: Graph of `Threads_connected`
- **Slow Query Count**: `Slow_queries` delta
- **Query Rate**: `Questions` per second

#### Row 3: Elasticsearch Metrics
- **Cluster Status**: Single stat (green/yellow/red)
- **Index Count**: Total indices
- **Document Count**: Total documents across all indices
- **JVM Heap Usage**: `heap_used_percent`

#### Row 4: Docker Container Metrics
- **Container CPU Usage**: Per container
- **Container Memory Usage**: Per container
- **Container Network I/O**: Bytes sent/received

### Dashboard 3: Business Metrics

**Purpose**: Track application-specific business metrics

#### Row 1: Person Operations
- **Total Persons**: Current count from `person.repository.count`
- **Persons Created (24h)**: `increase(person_created_total[24h])`
- **Persons Deleted (24h)**: `increase(person_deleted_total[24h])`
- **Validation Errors**: `rate(person_validation_error_total[5m])`

#### Row 2: User Activity
- **Active Sessions**: `tomcat_sessions_active_current`
- **Requests by Endpoint**: Pie chart of `http_server_requests_total` grouped by `uri`
- **Top Users by Activity**: If user tracking implemented

#### Row 3: Performance by Operation
- **Create Operation Duration**: Timer for create
- **Update Operation Duration**: Timer for update
- **Delete Operation Duration**: Timer for delete
- **Search Operation Duration**: Timer for findAll

### Simple HTML Dashboard (No External Tools)

If you don't have Grafana, you can create a simple HTML dashboard:

```html
<!DOCTYPE html>
<html>
<head>
    <title>Person Service Dashboard</title>
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; background: #f5f5f5; }
        .dashboard { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; }
        .panel { background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        .panel h3 { margin-top: 0; color: #333; }
        .metric-value { font-size: 36px; font-weight: bold; color: #0066cc; }
        .metric-label { color: #666; margin-top: 10px; }
        .status-green { color: #28a745; }
        .status-yellow { color: #ffc107; }
        .status-red { color: #dc3545; }
    </style>
</head>
<body>
    <h1>Person Service Dashboard</h1>
    <div class="dashboard">
        <!-- Service Health -->
        <div class="panel">
            <h3>Service Instances</h3>
            <div class="metric-value" id="instance-count">-</div>
            <div class="metric-label">Active Instances</div>
        </div>
        
        <!-- Request Rate -->
        <div class="panel">
            <h3>Total Requests</h3>
            <div class="metric-value" id="request-count">-</div>
            <div class="metric-label">Since Startup</div>
        </div>
        
        <!-- Memory Usage -->
        <div class="panel">
            <h3>Heap Memory</h3>
            <div class="metric-value" id="memory-usage">-</div>
            <div class="metric-label">MB Used</div>
        </div>
        
        <!-- DB Connections -->
        <div class="panel">
            <h3>DB Connections</h3>
            <div class="metric-value" id="db-connections">-</div>
            <div class="metric-label">Active</div>
        </div>
        
        <!-- Person Count -->
        <div class="panel">
            <h3>Total Persons</h3>
            <div class="metric-value" id="person-count">-</div>
            <div class="metric-label">In Database</div>
        </div>
        
        <!-- Consul Health -->
        <div class="panel">
            <h3>Consul Status</h3>
            <div class="metric-value" id="consul-status">-</div>
            <div class="metric-label">Service Registry</div>
        </div>
    </div>

    <script>
        function fetchMetrics() {
            // Service instance count from Consul
            $.get('http://localhost:8500/v1/catalog/service/person-service-client', function(data) {
                $('#instance-count').text(data.length);
            });
            
            // Request count from Actuator
            $.get('http://localhost:8081/actuator/metrics/http.server.requests', function(data) {
                const count = data.measurements.find(m => m.statistic === 'COUNT').value;
                $('#request-count').text(Math.floor(count));
            });
            
            // Memory usage
            $.get('http://localhost:8081/actuator/metrics/jvm.memory.used?tag=area:heap', function(data) {
                const bytes = data.measurements[0].value;
                const mb = (bytes / 1024 / 1024).toFixed(0);
                $('#memory-usage').text(mb);
            });
            
            // DB connections
            $.get('http://localhost:8081/actuator/metrics/hikaricp.connections.active', function(data) {
                $('#db-connections').text(data.measurements[0].value);
            });
            
            // Person count
            $.get('http://localhost:8081/actuator/metrics/person.repository.count', function(data) {
                $('#person-count').text(Math.floor(data.measurements[0].value));
            });
            
            // Consul health
            $.get('http://localhost:8500/v1/agent/self', function(data) {
                $('#consul-status').text('✅ UP').addClass('status-green');
            }).fail(function() {
                $('#consul-status').text('❌ DOWN').addClass('status-red');
            });
        }
        
        // Fetch metrics every 5 seconds
        fetchMetrics();
        setInterval(fetchMetrics, 5000);
    </script>
</body>
</html>
```

Save as `dashboard.html` and open in a browser. **Note**: You'll need to enable CORS on the services for this to work in production.

---

## 9. Alerting Strategy

### Alert Levels

#### P1 - Critical (Immediate Response Required)

**Criteria**: Service completely unavailable or data loss risk

**Examples**:
- All service instances down (0/3 healthy)
- Elasticsearch cluster RED status
- MySQL unavailable (connection failures)
- Error rate > 50% for more than 2 minutes

**Response Time**: Immediate (page on-call)

**Alert Configuration Example**:
```yaml
alert: AllServiceInstancesDown
expr: count(up{job="person-service"}) == 0
for: 1m
labels:
  severity: P1
  team: backend
annotations:
  summary: "All person-service instances are down"
  description: "No healthy instances of person-service-client detected in Consul"
  runbook: "https://wiki.example.com/runbooks/service-down"
```

#### P2 - High (Response Within 30 Minutes)

**Criteria**: Degraded service, not complete outage

**Examples**:
- 2 out of 3 service instances down
- Response time p99 > 5 seconds
- Error rate > 10% for more than 5 minutes
- Elasticsearch cluster YELLOW status for > 30 minutes
- Database connection pool > 90% utilized

**Response Time**: 30 minutes (alert team, investigate)

**Alert Configuration Example**:
```yaml
alert: HighErrorRate
expr: (sum(rate(http_server_requests_total{status=~"5.."}[5m])) / sum(rate(http_server_requests_total[5m]))) > 0.10
for: 5m
labels:
  severity: P2
  team: backend
annotations:
  summary: "Error rate above 10%"
  description: "Current error rate: {{ $value | humanizePercentage }}"
```

#### P3 - Medium (Response Within 2 Hours)

**Criteria**: Warning signs, not yet affecting users

**Examples**:
- Health check intermittently failing
- Memory usage > 80%
- Slow query count increasing
- Log volume anomalies (2x normal)
- GC pause time > 1 second

**Response Time**: 2 hours (business hours response)

**Alert Configuration Example**:
```yaml
alert: HighMemoryUsage
expr: (jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) > 0.80
for: 10m
labels:
  severity: P3
  team: backend
annotations:
  summary: "JVM heap memory usage above 80%"
  description: "Instance {{ $labels.instance }} is using {{ $value | humanizePercentage }} of heap"
```

#### P4 - Low (Monitor, Address During Business Hours)

**Criteria**: Informational, trend monitoring

**Examples**:
- Warning logs increasing
- Cache miss rate high
- Database connection pool usage > 60%
- Disk space > 70%

**Response Time**: Next business day

**Alert Configuration Example**:
```yaml
alert: ElevatedConnectionUsage
expr: (hikaricp_connections_active / hikaricp_connections) > 0.60
for: 30m
labels:
  severity: P4
  team: backend
annotations:
  summary: "Database connection pool usage elevated"
  description: "Connection pool is {{ $value | humanizePercentage }} utilized"
```

### Alert Destinations

**P1/P2 Alerts**:
- PagerDuty (or similar on-call system)
- SMS to on-call engineer
- Slack #incidents channel

**P3/P4 Alerts**:
- Slack #monitoring channel
- Email to team distribution list

### Alert Fatigue Prevention

**Best Practices**:

1. **Use `for` clauses**: Don't alert on transient spikes
   ```yaml
   expr: error_rate > 0.05
   for: 5m  # Must be true for 5 minutes
   ```

2. **Set appropriate thresholds**: Base on historical data
   - Baseline normal behavior first
   - Set thresholds 2-3 standard deviations from normal
   - Adjust based on false positive rate

3. **Group related alerts**: Don't alert on symptoms of the same root cause
   - If service is down, don't also alert on connection errors
   - Use alert dependencies

4. **Provide context**: Every alert should have
   - Clear summary
   - Current value
   - Link to runbook
   - Link to relevant dashboard

5. **Test alerts regularly**: Fire test alerts monthly to verify routing

### Runbook Example

Create runbooks for common alerts at `docs/runbooks/`:

**docs/runbooks/high-error-rate.md**:
```markdown
# Runbook: High Error Rate Alert

## Alert
- **Name**: HighErrorRate
- **Severity**: P2
- **Condition**: Error rate > 10% for 5 minutes

## Initial Response (First 5 minutes)

1. **Check service health in Consul**
   ```bash
   curl http://localhost:8500/v1/health/service/person-service-client?passing | jq 'length'
   ```
   Expected: 3 healthy instances

2. **Check recent deployments**
   - Did we deploy in the last hour?
   - If yes, consider rollback

3. **Check error logs in Kibana**
   - Go to Kibana Discover
   - Filter: `level:ERROR AND @timestamp:[now-15m TO now]`
   - Look for patterns in exception types

## Common Causes

### Database Connection Errors
**Symptoms**: `java.sql.SQLException` in logs
**Fix**:
```bash
# Check MySQL health
docker compose ps mysql
docker compose logs mysql

# Restart MySQL if needed
docker compose restart mysql
```

### Consul Unavailable
**Symptoms**: `ConsulException: Connection refused`
**Fix**:
```bash
# Check Consul
docker compose ps consul
docker compose restart consul
```

### Out of Memory
**Symptoms**: `java.lang.OutOfMemoryError` in logs
**Fix**:
```bash
# Restart affected service instance
docker compose restart service-1

# Investigate memory leak
# Take heap dump when issue recurs
```

## Escalation
- If issue not resolved in 30 minutes: Page senior engineer
- If data loss risk: Page engineering manager
```

---

## 10. Metric Retention & Storage

### Retention Strategy

Different metrics need different retention periods:

#### Short-term (1-7 days) - High Resolution
- **Granularity**: 1-second intervals
- **Purpose**: Real-time monitoring, recent troubleshooting
- **Metrics**: All metrics
- **Storage**: In-memory or fast SSD

**Example metrics**:
- HTTP request count (1s resolution)
- JVM heap usage (1s resolution)
- Database connection pool (1s resolution)

#### Medium-term (7-90 days) - Aggregated
- **Granularity**: 1-minute intervals
- **Purpose**: Trend analysis, capacity planning
- **Metrics**: Core metrics only
- **Storage**: Time-series database (InfluxDB, Prometheus)

**Example metrics**:
- Average response time (1m resolution)
- Error rate (1m resolution)
- Memory usage average (1m resolution)

#### Long-term (1+ year) - Summarized
- **Granularity**: 1-hour intervals
- **Purpose**: Historical analysis, yearly reports
- **Metrics**: Business metrics and KPIs
- **Storage**: Data warehouse or long-term time-series storage

**Example metrics**:
- Daily active users
- Total persons created per day
- Service availability percentage
- Cost per request

### Implementing Retention with Prometheus

**prometheus.yml**:
```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

# Storage retention
storage:
  tsdb:
    retention.time: 15d  # Keep raw metrics for 15 days
    retention.size: 10GB  # Or until 10GB used

# Remote write for long-term storage (optional)
remote_write:
  - url: http://long-term-storage:9201/write
    queue_config:
      capacity: 10000
      max_samples_per_send: 1000
```

### Implementing Retention with InfluxDB

**influxdb.conf**:
```toml
[data]
  # Shard duration (how long data stays in hot storage)
  cache-max-memory-size = 1073741824  # 1GB

[retention]
  enabled = true
  check-interval = "30m"

# Retention policies
CREATE RETENTION POLICY "1_day" ON "metrics" DURATION 1d REPLICATION 1
CREATE RETENTION POLICY "7_days" ON "metrics" DURATION 7d REPLICATION 1
CREATE RETENTION POLICY "90_days" ON "metrics" DURATION 90d REPLICATION 1

# Continuous queries for downsampling
CREATE CONTINUOUS QUERY "cq_1m" ON "metrics"
BEGIN
  SELECT mean(value) AS value
  INTO "7_days".:MEASUREMENT
  FROM "1_day".:MEASUREMENT
  GROUP BY time(1m), *
END

CREATE CONTINUOUS QUERY "cq_1h" ON "metrics"
BEGIN
  SELECT mean(value) AS value
  INTO "90_days".:MEASUREMENT
  FROM "7_days".:MEASUREMENT
  GROUP BY time(1h), *
END
```

### Storage Size Estimation

**Formula**:
```
Storage Size = (Metric Count × Cardinality × Sample Rate × Retention Period × Bytes per Sample)
```

**Example for our application**:
- **Metric count**: 100 unique metrics
- **Cardinality**: 5 (3 service instances + 1 frontend + averages)
- **Sample rate**: 1 sample per 15 seconds = 4 samples/minute
- **Retention period**: 15 days
- **Bytes per sample**: ~50 bytes (timestamp + value + labels)

```
Size = 100 × 5 × 4 × 60 × 24 × 15 × 50 bytes
     = 100 × 5 × 4 × 21600 × 50 bytes
     = 2,160,000,000 bytes
     = 2.16 GB for 15 days
```

Add 20% overhead for indexing: **~2.6 GB total**

### Backup Strategy

**Daily backups** of metrics data:
```bash
#!/bin/bash
# backup-metrics.sh

DATE=$(date +%Y%m%d)
BACKUP_DIR="/backups/metrics"

# Backup Prometheus data
tar -czf "$BACKUP_DIR/prometheus-$DATE.tar.gz" /var/lib/prometheus

# Upload to S3 (if using AWS)
aws s3 cp "$BACKUP_DIR/prometheus-$DATE.tar.gz" s3://my-bucket/metrics/

# Delete backups older than 30 days
find "$BACKUP_DIR" -name "prometheus-*.tar.gz" -mtime +30 -delete
```

---

## 11. Integrating with External Monitoring

### Prometheus Integration

#### Step 1: Add Prometheus Registry Dependency

**pom.xml**:
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

#### Step 2: Enable Prometheus Endpoint

**application.yml**:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

#### Step 3: Verify Prometheus Endpoint

```bash
curl http://localhost:8081/actuator/prometheus

# Response (Prometheus format)
# HELP jvm_memory_used_bytes The amount of used memory
# TYPE jvm_memory_used_bytes gauge
jvm_memory_used_bytes{area="heap",id="G1 Eden Space"} 1.25829120E8
jvm_memory_used_bytes{area="heap",id="G1 Old Gen"} 6.7108864E7

# HELP http_server_requests_seconds  
# TYPE http_server_requests_seconds summary
http_server_requests_seconds_count{exception="None",method="GET",status="200",uri="/people"} 152.0
http_server_requests_seconds_sum{exception="None",method="GET",status="200",uri="/people"} 7.656
```

#### Step 4: Configure Prometheus to Scrape

**prometheus.yml**:
```yaml
scrape_configs:
  - job_name: 'person-service'
    consul_sd_configs:
      - server: 'consul:8500'
        services:
          - person-service-client
    metrics_path: '/actuator/prometheus'
    scrape_interval: 15s
    scrape_timeout: 10s

  - job_name: 'person-front'
    consul_sd_configs:
      - server: 'consul:8500'
        services:
          - person-front
    metrics_path: '/actuator/prometheus'
    scrape_interval: 15s
```

**Note**: Prometheus uses Consul for service discovery, automatically finding all instances!

#### Step 5: Add to Docker Compose

**docker-compose.yml**:
```yaml
prometheus:
  image: prom/prometheus:latest
  container_name: prometheus
  volumes:
    - ./prometheus-config:/etc/prometheus
    - prometheus-data:/prometheus
  command:
    - '--config.file=/etc/prometheus/prometheus.yml'
    - '--storage.tsdb.path=/prometheus'
    - '--storage.tsdb.retention.time=15d'
  ports:
    - "9090:9090"
  depends_on:
    - consul

volumes:
  prometheus-data:
```

### Grafana Integration

#### Step 1: Add to Docker Compose

**docker-compose.yml**:
```yaml
grafana:
  image: grafana/grafana:latest
  container_name: grafana
  environment:
    - GF_SECURITY_ADMIN_PASSWORD=admin
    - GF_USERS_ALLOW_SIGN_UP=false
  volumes:
    - grafana-data:/var/lib/grafana
    - ./grafana-provisioning:/etc/grafana/provisioning
  ports:
    - "3000:3000"
  depends_on:
    - prometheus

volumes:
  grafana-data:
```

#### Step 2: Create Datasource Configuration

**grafana-provisioning/datasources/prometheus.yml**:
```yaml
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: false
```

#### Step 3: Import Spring Boot Dashboard

1. Access Grafana at http://localhost:3000
2. Login with admin/admin
3. Go to Dashboards → Import
4. Enter dashboard ID: **4701** (JVM Micrometer)
5. Select Prometheus datasource
6. Click Import

**Popular Grafana Dashboards for Spring Boot**:
- **4701**: JVM (Micrometer)
- **6756**: Spring Boot Statistics
- **11378**: Spring Boot 2.1 System Monitor
- **12900**: Spring Boot Observability

### AWS CloudWatch Integration

#### Step 1: Add CloudWatch Dependency

**pom.xml**:
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-cloudwatch2</artifactId>
</dependency>
```

#### Step 2: Configure CloudWatch

**application.yml**:
```yaml
management:
  metrics:
    export:
      cloudwatch:
        namespace: PersonService
        batch-size: 20
        enabled: true
        step: 1m
```

#### Step 3: Set AWS Credentials

```bash
export AWS_ACCESS_KEY_ID=your-access-key
export AWS_SECRET_ACCESS_KEY=your-secret-key
export AWS_REGION=us-east-1
```

Metrics will now appear in AWS CloudWatch under the "PersonService" namespace.

### Datadog Integration

**pom.xml**:
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-datadog</artifactId>
</dependency>
```

**application.yml**:
```yaml
management:
  metrics:
    export:
      datadog:
        api-key: ${DATADOG_API_KEY}
        application-key: ${DATADOG_APP_KEY}
        enabled: true
        step: 10s
```

---

## 12. Performance Baseline Establishment

### Why Establish a Baseline?

A performance baseline is a snapshot of your system's **normal behavior** under typical conditions. Without a baseline, you can't answer questions like:
- Is this response time normal?
- Is memory usage higher than usual?
- Are we handling more traffic than before?

### Baseline Metrics to Capture

#### 1. Response Time Under Normal Load

**Scenario**: 10 concurrent users, mixed read/write operations

**Test script** (`load-test.sh`):
```bash
#!/bin/bash
# Simple load test using Apache Bench

URL="http://localhost:8080"
REQUESTS=1000
CONCURRENCY=10

echo "=== Load Test: Normal Load ==="
echo "URL: $URL"
echo "Requests: $REQUESTS"
echo "Concurrency: $CONCURRENCY"
echo ""

# Test GET /people
echo "Testing GET /people..."
ab -n $REQUESTS -c $CONCURRENCY "$URL/people" > results-get-people.txt

# Extract key metrics
echo "Results:"
grep "Requests per second" results-get-people.txt
grep "Time per request" results-get-people.txt
grep "50%\|95%\|99%" results-get-people.txt
```

**Baseline results** (capture these):
```
Requests per second: 245.32 [#/sec] (mean)
Time per request: 40.781 [ms] (mean)
Time per request: 4.078 [ms] (mean, across all concurrent requests)

Percentage of requests served within a certain time (ms)
  50%: 35
  66%: 38
  75%: 42
  80%: 45
  90%: 58
  95%: 78
  98%: 125
  99%: 156
 100%: 234 (longest request)
```

**Document**: Save these results to `docs/baselines/normal-load.md`

#### 2. Maximum Throughput

**Scenario**: How many requests/second can the system handle?

**Test script** (`max-throughput.sh`):
```bash
#!/bin/bash
# Find maximum throughput using wrk

URL="http://localhost:8080/people"

echo "=== Finding Maximum Throughput ==="
echo ""

for THREADS in 2 4 8 16; do
    for CONNECTIONS in 50 100 200 400; do
        echo "Testing: $THREADS threads, $CONNECTIONS connections"
        wrk -t$THREADS -c$CONNECTIONS -d30s "$URL" 2>&1 | grep "Requests/sec"
    done
done
```

**Baseline result**:
```
Maximum sustained throughput: 1,250 requests/sec
CPU usage at max: 75%
Memory usage at max: 450 MB
```

#### 3. Memory Usage Patterns

**Capture over 1 hour**:
```bash
#!/bin/bash
# memory-baseline.sh

SERVICE="localhost:8081"
DURATION=3600  # 1 hour
INTERVAL=60    # 1 minute

echo "timestamp,heap_used_mb,heap_max_mb,heap_percent" > memory-baseline.csv

for ((i=1; i<=DURATION/INTERVAL; i++)); do
    TIMESTAMP=$(date +%s)
    
    HEAP_USED=$(curl -s "http://$SERVICE/actuator/metrics/jvm.memory.used?tag=area:heap" | \
        jq '.measurements[0].value / 1024 / 1024')
    
    HEAP_MAX=$(curl -s "http://$SERVICE/actuator/metrics/jvm.memory.max?tag=area:heap" | \
        jq '.measurements[0].value / 1024 / 1024')
    
    HEAP_PERCENT=$(echo "scale=2; $HEAP_USED / $HEAP_MAX * 100" | bc)
    
    echo "$TIMESTAMP,$HEAP_USED,$HEAP_MAX,$HEAP_PERCENT" >> memory-baseline.csv
    
    sleep $INTERVAL
done

echo "Baseline captured in memory-baseline.csv"
```

**Analyze results**:
```bash
# Calculate average, min, max
awk -F',' 'NR>1 {sum+=$2; if($2>max) max=$2; if(NR==2 || $2<min) min=$2} END {print "Avg:", sum/(NR-1), "Min:", min, "Max:", max}' memory-baseline.csv
```

**Expected results**:
```
Average heap used: 285 MB
Min heap used: 120 MB (after GC)
Max heap used: 450 MB (before GC)
```

#### 4. Database Query Performance

**Baseline key queries**:
```sql
-- Timing: SELECT all persons
SET profiling = 1;
SELECT * FROM person;
SHOW PROFILE;

-- Expected: < 10ms for 100 rows

-- Timing: SELECT by ID (indexed)
SELECT * FROM person WHERE id = 1;
SHOW PROFILE;

-- Expected: < 1ms

-- Timing: SELECT by email (indexed)
SELECT * FROM person WHERE email = 'test@example.com';
SHOW PROFILE;

-- Expected: < 2ms

-- Timing: COUNT
SELECT COUNT(*) FROM person;
SHOW PROFILE;

-- Expected: < 5ms
```

**Document results** in `docs/baselines/database-queries.md`

#### 5. Startup Time

**Measure cold start**:
```bash
#!/bin/bash
# startup-baseline.sh

docker compose down
docker compose up -d service-1

echo "Waiting for service to start..."
START=$(date +%s)

until curl -s http://localhost:8081/actuator/health | grep -q "UP"; do
    sleep 1
done

END=$(date +%s)
DURATION=$((END - START))

echo "Service started in $DURATION seconds"
```

**Expected baseline**:
```
Cold start (first time): 45-60 seconds
Warm start (restart): 20-30 seconds
```

### Creating a Baseline Document

**docs/baselines/performance-baseline.md**:
```markdown
# Performance Baseline

**Date**: April 20, 2026  
**Version**: 1.0-SNAPSHOT (Spring Boot 3.2.3)  
**Environment**: Docker Compose on MacBook Pro (M1, 16GB RAM)  
**Configuration**: 3 backend instances, 1 frontend, MySQL 8.0

## Response Time

| Endpoint | p50 | p95 | p99 | Max |
|----------|-----|-----|-----|-----|
| GET /people | 35ms | 78ms | 156ms | 234ms |
| GET /people/{id} | 12ms | 25ms | 45ms | 89ms |
| POST /people | 45ms | 95ms | 180ms | 310ms |
| PUT /people | 48ms | 98ms | 185ms | 320ms |
| DELETE /people/{id} | 20ms | 42ms | 78ms | 125ms |

## Throughput

- **Normal load (10 concurrent)**: 245 req/sec
- **Maximum sustained**: 1,250 req/sec
- **Peak burst**: 1,800 req/sec (for < 30 seconds)

## Resource Usage

### Memory (per instance)
- **Idle**: 120-150 MB heap
- **Normal load**: 250-350 MB heap
- **Peak load**: 400-450 MB heap
- **Max configured**: 512 MB heap

### CPU
- **Idle**: 2-5%
- **Normal load**: 15-25%
- **Peak load**: 60-75%

### Database Connections
- **Idle**: 2 active, 8 idle
- **Normal load**: 4-6 active, 4-6 idle
- **Peak load**: 8-9 active, 1-2 idle
- **Pool size**: 10 connections

## Database Query Performance

| Query | Average | p95 |
|-------|---------|-----|
| SELECT * FROM person | 8ms | 15ms |
| SELECT ... WHERE id = ? | 0.8ms | 1.5ms |
| SELECT ... WHERE email = ? | 1.2ms | 2.5ms |
| INSERT INTO person | 5ms | 12ms |
| UPDATE person | 6ms | 14ms |
| DELETE FROM person | 3ms | 8ms |

## Startup Time

- **Cold start**: 50 seconds
- **Warm start**: 25 seconds

## Expected Behavior

### Normal Patterns
- Heap memory: Sawtooth pattern, GC every 2-5 minutes
- Response time: Consistent, occasional spikes < 500ms
- Error rate: < 0.1%
- Connection pool: 60-70% idle capacity

### Thresholds for Alerts
- Response time p95 > 200ms: Investigate
- Response time p99 > 500ms: Alert
- Error rate > 1%: Alert
- Memory > 80% heap: Warning
- Memory > 90% heap: Critical
```

### Using the Baseline

**Compare current metrics against baseline**:
```bash
# Current p95 response time
CURRENT=$(curl -s 'http://localhost:8081/actuator/metrics/http.server.requests?tag=uri:/people' | \
    jq '.measurements[] | select(.statistic=="TOTAL_TIME").value')

BASELINE=0.078  # 78ms from baseline

# Calculate deviation
DEVIATION=$(echo "scale=2; (($CURRENT - $BASELINE) / $BASELINE) * 100" | bc)

echo "Current: ${CURRENT}s"
echo "Baseline: ${BASELINE}s"
echo "Deviation: ${DEVIATION}%"

if (( $(echo "$DEVIATION > 50" | bc -l) )); then
    echo "⚠️  Performance degraded by more than 50%!"
fi
```

---

## Summary

This comprehensive guide covers monitoring and metrics for the Person Service microservices application:

1. **Metrics Fundamentals**: Understanding counters, gauges, histograms, and timers
2. **Spring Boot Actuator**: Leveraging built-in metrics for JVM, HTTP, database, and Tomcat
3. **Custom Metrics**: Adding business-specific metrics using Micrometer
4. **Infrastructure Metrics**: Monitoring Consul, MySQL, and Elasticsearch
5. **Trend Interpretation**: Recognizing normal vs abnormal patterns
6. **Dashboards**: Creating visual representations of system health
7. **Alerting**: Implementing a tiered alerting strategy (P1-P4)
8. **Retention**: Managing metric storage across time horizons
9. **External Integration**: Connecting to Prometheus, Grafana, and cloud monitoring
10. **Baselines**: Establishing performance benchmarks for comparison

### Key Takeaways

- **Metrics + Logs = Complete Observability**: Use both together
- **Baselines are Essential**: You can't identify anomalies without knowing normal
- **Alert Thoughtfully**: Avoid alert fatigue with proper thresholds and grouping
- **Dashboards Should Tell a Story**: Top-to-bottom, critical to informational
- **Monitor the Whole Stack**: Application, infrastructure, and business metrics

### Next Steps

1. **Enable Prometheus endpoint** in the application
2. **Create initial dashboards** (start simple, add complexity)
3. **Establish baselines** by running load tests
4. **Configure alerts** for P1/P2 scenarios
5. **Document runbooks** for common issues
6. **Review and adjust** thresholds based on real-world data

---

**Related Documentation**:
- [Consul Service Discovery Guide](./consul-service-discovery.md)
- [ELK Stack Centralized Logging Guide](./elk-stack-logging.md)
- [Docker Commands Reference](./docker-commands.md)

**External Resources**:
- [Spring Boot Actuator Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Micrometer Documentation](https://micrometer.io/docs)
- [Prometheus Best Practices](https://prometheus.io/docs/practices/)
- [Grafana Dashboards](https://grafana.com/grafana/dashboards)
