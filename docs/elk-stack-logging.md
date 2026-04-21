# ELK Stack Centralized Logging Guide

**Version**: 1.0  
**Last Updated**: April 20, 2026  
**ELK Version**: 7.10.2  
**Spring Boot Version**: 3.2.3

---

## Table of Contents

1. [Introduction to ELK Stack](#introduction-to-elk-stack)
2. [ELK Components Overview](#elk-components-overview)
3. [How Our Application Uses ELK](#how-our-application-uses-elk)
4. [Logging Configuration](#logging-configuration)
5. [Logstash Pipeline Configuration](#logstash-pipeline-configuration)
6. [Kibana Setup & Usage](#kibana-setup--usage)
7. [Creating Visualizations](#creating-visualizations)
8. [Monitoring Application Health via Logs](#monitoring-application-health-via-logs)
9. [Log Best Practices](#log-best-practices)
10. [Troubleshooting ELK Stack](#troubleshooting-elk-stack)
11. [Performance Considerations](#performance-considerations)

---

## Introduction to ELK Stack

**ELK Stack** is a collection of three open-source products — **Elasticsearch**, **Logstash**, and **Kibana** — all developed and maintained by Elastic. Together, they provide a powerful platform for centralized logging, search, and visualization.

### What is Centralized Logging?

In a microservices architecture, you have multiple service instances running across different containers or servers. Each generates its own logs. **Centralized logging** aggregates all these logs into a single location, making it easier to:

- 🔍 **Search** across all services simultaneously
- 📊 **Analyze** patterns and trends
- 🚨 **Detect** errors and anomalies
- 🐛 **Debug** issues without SSH access to servers
- 📈 **Monitor** application health in real-time

### Why ELK for Microservices?

```
Traditional Logging (Distributed)          Centralized Logging (ELK)
────────────────────────────              ──────────────────────────

Service-1 → file1.log                     Service-1 ┐
Service-2 → file2.log                     Service-2 ├─→ Logstash → Elasticsearch
Service-3 → file3.log                     Service-3 ┘                    ↓
                                          Frontend ──────────────────→  Kibana
❌ Must SSH to each server                              ↓
❌ Grep through separate files            ✅ Single web interface
❌ No correlation between services        ✅ Unified search across all logs
❌ Difficult to spot patterns             ✅ Real-time visualization
❌ Manual log rotation                    ✅ Automatic index management
```

### Benefits for This Project

1. **Distributed Tracing**: Follow a request through Frontend → Service-1 → MySQL → Response
2. **Error Aggregation**: See all errors from all 3 backend instances in one view
3. **Performance Monitoring**: Identify slow queries or endpoints
4. **Historical Analysis**: Keep logs for weeks/months for trend analysis
5. **Real-time Alerting**: Get notified when error rates spike
6. **No Server Access Needed**: Developers view logs via web UI

---

## ELK Components Overview

### Elasticsearch

**What**: A distributed, RESTful search and analytics engine built on Apache Lucene.

**Purpose in ELK**: Storage and search engine for all log data.

**Key Features**:
- 📦 **Document Store**: Stores logs as JSON documents
- 🔎 **Full-Text Search**: Fast search across millions of log entries
- ⚡ **Near Real-Time**: Index and search with minimal latency (<1 second)
- 📊 **Aggregations**: Analyze data (count, average, percentiles, etc.)
- 🌍 **Distributed**: Horizontally scalable across multiple nodes
- 🔄 **Automatic Sharding**: Distributes data across shards for performance

**Architecture**:
```
┌─────────────────────────────────────────┐
│         Elasticsearch Cluster            │
├─────────────────────────────────────────┤
│  Node 1 (Master + Data)                 │
│  ┌──────────┬──────────┬──────────┐    │
│  │ Index 1  │ Index 2  │ Index 3  │    │
│  │ (Logs)   │ (Logs)   │ (Logs)   │    │
│  │ Apr 18   │ Apr 19   │ Apr 20   │    │
│  └──────────┴──────────┴──────────┘    │
│                                          │
│  Indices rotate daily (logstash-*)      │
└─────────────────────────────────────────┘
```

**Data Model**:
```json
{
  "_index": "logstash-2026.04.20",
  "_type": "_doc",
  "_id": "abc123",
  "_source": {
    "@timestamp": "2026-04-20T19:45:23.123Z",
    "message": "User created successfully",
    "level": "INFO",
    "logger_name": "io.github.joxebus.service.PersonService",
    "thread_name": "http-nio-8081-exec-5",
    "application_name": "person-service-client",
    "stack_trace": null
  }
}
```

**API Examples**:
```bash
# Cluster health
curl http://localhost:9200/_cluster/health?pretty

# List indices
curl http://localhost:9200/_cat/indices?v

# Search logs
curl http://localhost:9200/logstash-*/_search?q=level:ERROR

# Count documents
curl http://localhost:9200/logstash-*/_count
```

### Logstash

**What**: A server-side data processing pipeline that ingests, transforms, and sends data.

**Purpose in ELK**: Collect logs from applications, parse/transform them, and send to Elasticsearch.

**Key Features**:
- 📥 **Multiple Inputs**: TCP, UDP, files, databases, message queues
- 🔄 **Transformation**: Parse, filter, enrich, and modify log data
- 📤 **Multiple Outputs**: Elasticsearch, files, databases, monitoring systems
- 🔌 **Plugins**: 200+ community plugins for various integrations

**Pipeline Architecture**:
```
┌──────────┐      ┌──────────┐      ┌──────────┐
│  INPUT   │  →   │  FILTER  │  →   │  OUTPUT  │
├──────────┤      ├──────────┤      ├──────────┤
│ TCP 4560 │      │  Parse   │      │ Elastic- │
│ (JSON)   │      │  Mutate  │      │ search   │
│          │      │  Enrich  │      │ :9200    │
└──────────┘      └──────────┘      └──────────┘
```

**Example Pipeline**:
```ruby
input {
  tcp {
    port => 4560
    codec => json_lines
  }
}

filter {
  # Add environment tag
  mutate {
    add_field => { "environment" => "docker" }
  }
  
  # Parse timestamp
  date {
    match => [ "@timestamp", "ISO8601" ]
  }
}

output {
  elasticsearch {
    hosts => ["elasticsearch:9200"]
    index => "logstash-%{+YYYY.MM.dd}"
  }
}
```

### Kibana

**What**: A web-based visualization and exploration tool for Elasticsearch data.

**Purpose in ELK**: User interface for searching logs, creating visualizations, and building dashboards.

**Key Features**:
- 🔍 **Discover**: Search and filter logs with KQL (Kibana Query Language)
- 📊 **Visualize**: Create charts, graphs, maps, and metrics
- 📈 **Dashboard**: Combine multiple visualizations into dashboards
- 🔔 **Alerting**: Set up notifications for specific conditions
- 🔧 **Dev Tools**: Console for Elasticsearch queries
- ⚙️ **Management**: Index patterns, saved objects, settings

**Main Views**:

1. **Discover** - Search and explore log data
2. **Visualize** - Create charts and graphs
3. **Dashboard** - Combine visualizations
4. **Canvas** - Infographic-style presentations
5. **Maps** - Geospatial data visualization
6. **Machine Learning** - Anomaly detection

---

## How Our Application Uses ELK

### Architecture Flow

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  Spring Boot    │     │  Spring Boot    │     │  Spring Boot    │
│  Service-1:8081 │     │  Service-2:8082 │     │  Service-3:8083 │
└────────┬────────┘     └────────┬────────┘     └────────┬────────┘
         │                       │                       │
         │  logstash-logback-encoder (JSON)             │
         │                       │                       │
         └───────────────────────┴───────────────────────┘
                                 ↓ TCP :4560
                        ┌─────────────────┐
                        │    Logstash     │
                        │   Port: 4560    │
                        └────────┬────────┘
                                 ↓ HTTP :9200
                        ┌─────────────────┐
                        │  Elasticsearch  │
                        │   Port: 9200    │
                        │                 │
                        │  ┌───────────┐  │
                        │  │logstash-  │  │
                        │  │2026.04.18 │  │
                        │  ├───────────┤  │
                        │  │logstash-  │  │
                        │  │2026.04.19 │  │
                        │  ├───────────┤  │
                        │  │logstash-  │  │
                        │  │2026.04.20 │  │
                        │  └───────────┘  │
                        └────────┬────────┘
                                 ↓ HTTP :5601
                        ┌─────────────────┐
                        │     Kibana      │
                        │   Port: 5601    │
                        │   (Web UI)      │
                        └─────────────────┘
                                 ↑
                          ┌──────┴──────┐
                          │  Developers │
                          │   Browser   │
                          └─────────────┘
```

### Data Flow Example

**1. Application Logs Something**:
```groovy
log.info("Person created: ${person}")
```

**2. Logback Encoder Creates JSON**:
```json
{
  "@timestamp": "2026-04-20T19:45:23.123Z",
  "@version": "1",
  "message": "Person created: Person{id=5, name='John', lastname='Doe', email='john@example.com'}",
  "logger_name": "io.github.joxebus.controller.PersonController",
  "thread_name": "http-nio-8081-exec-5",
  "level": "INFO",
  "level_value": 20000,
  "application_name": "person-service-client"
}
```

**3. Sent to Logstash via TCP**:
```
Connection: service-1:random-port → logstash:4560
Protocol: TCP with keep-alive
Format: JSON newline-delimited
```

**4. Logstash Processes and Enriches**:
```json
{
  "@timestamp": "2026-04-20T19:45:23.123Z",
  "message": "Person created: ...",
  "level": "INFO",
  "application_name": "person-service-client",
  "environment": "docker",  // ← Added by Logstash
  "service_instance": "service-1"  // ← Added by Logstash
}
```

**5. Indexed in Elasticsearch**:
```
Index: logstash-2026.04.20
Document ID: abc123xyz
Indexed in: 50ms
```

**6. Queryable in Kibana**:
```
User opens Kibana → Searches "Person created" → Finds log entry
Time elapsed: <1 second
```

### Log Volume

In our setup with 3 backend services + 1 frontend:

```
Typical Log Volume (per minute):
- Service-1: ~100 log lines
- Service-2: ~100 log lines
- Service-3: ~100 log lines
- Frontend: ~50 log lines
────────────────────────────────
Total: ~350 log lines/minute

Daily Volume:
- 350 lines/min × 60 min × 24 hours = ~504,000 log lines/day
- Average size: ~500 bytes per log line
- Daily storage: ~250 MB/day (before compression)
- With compression: ~75 MB/day
```

### Index Rotation

Indices rotate daily to keep query performance optimal:

```
logstash-2026.04.18  (2 days old)   →  Can delete after 30 days
logstash-2026.04.19  (1 day old)    →  Searchable
logstash-2026.04.20  (current)      →  Actively writing
```

---

## Logging Configuration

### Spring Boot Dependencies

#### pom.xml

```xml
<!-- Logstash Logback Encoder -->
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>

<!-- Spring Boot Starter (includes Logback) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter</artifactId>
</dependency>
```

### Logback Configuration

#### log-config.xml (Backend Service)

Located at: `spring-boot-service/src/main/resources/log-config.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- Console Appender (for local development) -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd'T'HH:mm:ss.SSS} %5p %6([%thread]) %-40.40logger{39} : %msg%n</pattern>
        </encoder>
    </appender>

    <!-- File Appender (local logs) -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/application.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/application-%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
            <totalSizeCap>1GB</totalSizeCap>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- Logstash Appender (sends to ELK) -->
    <appender name="LOGSTASH" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
        <destination>${logstash.host:-localhost}:4560</destination>
        
        <!-- Reconnection delay if connection fails -->
        <reconnectionDelay>10 second</reconnectionDelay>
        
        <!-- Write timeout -->
        <writeTimeout>1 minute</writeTimeout>
        
        <!-- Keep connection alive -->
        <keepAliveDuration>5 minutes</keepAliveDuration>
        
        <!-- Encoder with custom fields -->
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <!-- Add application name -->
            <customFields>{"application_name":"person-service-client"}</customFields>
            
            <!-- Include caller data (expensive, use sparingly) -->
            <includeCallerData>false</includeCallerData>
            
            <!-- Include context (MDC) -->
            <includeContext>true</includeContext>
            
            <!-- Include structured arguments -->
            <includeStructuredArguments>true</includeStructuredArguments>
            
            <!-- Field names -->
            <fieldNames>
                <timestamp>@timestamp</timestamp>
                <version>@version</version>
                <message>message</message>
                <logger>logger_name</logger>
                <thread>thread_name</thread>
                <level>level</level>
                <levelValue>level_value</levelValue>
                <stackTrace>stack_trace</stackTrace>
            </fieldNames>
        </encoder>
        
        <!-- Async sending to avoid blocking application threads -->
        <ringBufferSize>8192</ringBufferSize>
    </appender>

    <!-- Async wrapper for better performance -->
    <appender name="ASYNC_LOGSTASH" class="ch.qos.logback.classic.AsyncAppender">
        <appender-ref ref="LOGSTASH"/>
        <queueSize>512</queueSize>
        <discardingThreshold>0</discardingThreshold>
        <includeCallerData>false</includeCallerData>
    </appender>

    <!-- Logger levels -->
    <logger name="io.github.joxebus" level="INFO"/>
    <logger name="org.springframework" level="WARN"/>
    <logger name="org.hibernate" level="WARN"/>
    <logger name="com.zaxxer.hikari" level="WARN"/>

    <!-- Root logger -->
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
        <appender-ref ref="ASYNC_LOGSTASH"/>
    </root>
</configuration>
```

### Configuration Breakdown

#### Logstash Appender Properties

| Property | Value | Description |
|----------|-------|-------------|
| `destination` | `logstash:4560` | Logstash TCP input endpoint |
| `reconnectionDelay` | `10 second` | Wait before reconnecting after failure |
| `writeTimeout` | `1 minute` | Max time to send a log entry |
| `keepAliveDuration` | `5 minutes` | How long to keep connection alive |
| `ringBufferSize` | `8192` | Internal buffer size for batching |

#### Custom Fields

```json
{
  "application_name": "person-service-client",
  "service_instance": "${HOSTNAME}",
  "environment": "${ENVIRONMENT:-development}"
}
```

**Adding custom fields**:
```xml
<customFields>{
  "application_name":"person-service-client",
  "service_version":"${project.version}",
  "datacenter":"us-west-2"
}</customFields>
```

#### Field Mappings

Maps Logback fields to Elasticsearch fields:

```xml
<fieldNames>
  <timestamp>@timestamp</timestamp>      <!-- ISO 8601 timestamp -->
  <version>@version</version>            <!-- Always "1" -->
  <message>message</message>              <!-- Log message -->
  <logger>logger_name</logger>            <!-- Java class name -->
  <thread>thread_name</thread>            <!-- Thread name -->
  <level>level</level>                    <!-- DEBUG/INFO/WARN/ERROR -->
  <levelValue>level_value</levelValue>    <!-- Numeric level -->
  <stackTrace>stack_trace</stackTrace>    <!-- Exception stack trace -->
</fieldNames>
```

### Application Properties

#### application.yml

```yaml
# Logstash host configuration
logstash:
  host: ${LOGSTASH_HOST:localhost}

logging:
  config: classpath:log-config.xml
  level:
    root: INFO
    io.github.joxebus: INFO
    org.springframework: WARN
    org.hibernate: WARN
```

#### application-docker.yml

```yaml
logstash:
  host: logstash  # Docker service name

logging:
  config: classpath:log-config.xml
  level:
    io.github.joxebus: DEBUG  # More verbose in Docker for troubleshooting
```

### Log Levels Explained

| Level | Value | Usage | Example |
|-------|-------|-------|---------|
| **TRACE** | 5000 | Very detailed, usually only for diagnosis | Method entry/exit |
| **DEBUG** | 10000 | Detailed information for debugging | Variable values, flow control |
| **INFO** | 20000 | Important business events | User created, order placed |
| **WARN** | 30000 | Warning but not error | Deprecated API used, retry attempt |
| **ERROR** | 40000 | Error that doesn't stop application | Failed to send email, query timeout |

**Best Practice**: Use INFO for business events, WARN for recoverable issues, ERROR for serious problems.

### Logging in Code

#### Good Logging Examples

```groovy
import groovy.util.logging.Slf4j

@Slf4j
@RestController
class PersonController {
    
    // ✅ Good: Logs important business event
    @PostMapping("/people")
    def createPerson(@RequestBody Person person) {
        log.info("Creating person: ${person.email}")
        
        def created = personService.save(person)
        
        log.info("Person created successfully: id=${created.id}")
        return created
    }
    
    // ✅ Good: Logs error with context
    @DeleteMapping("/people/{id}")
    def deletePerson(@PathVariable Long id) {
        try {
            personService.delete(id)
            log.info("Person deleted: id=${id}")
            return ResponseEntity.ok().build()
        } catch (Exception e) {
            log.error("Failed to delete person: id=${id}", e)
            return ResponseEntity.badRequest().build()
        }
    }
    
    // ✅ Good: Uses parameterized logging (efficient)
    def updatePerson(Person person) {
        log.debug("Updating person: id={}, email={}", person.id, person.email)
        // ...
    }
}
```

#### Bad Logging Examples

```groovy
// ❌ Bad: Logging in tight loop (too verbose)
persons.each { person ->
    log.debug("Processing person: ${person}")  // Don't do this
}

// ❌ Bad: Logging sensitive data
log.info("User login: username=${username}, password=${password}")

// ❌ Bad: String concatenation (inefficient)
log.debug("Person details: " + person.toString())  // Use parameterized instead

// ❌ Bad: Redundant ERROR logging
try {
    // ...
} catch (Exception e) {
    log.error("Error occurred", e)
    throw e  // Exception will be logged again at higher level
}

// ❌ Bad: Logging without context
log.error("Failed to save")  // What failed? What was being saved?
```

### Structured Logging

Add structure to logs for better querying:

```groovy
import net.logstash.logback.argument.StructuredArguments

// Add structured fields
log.info("Person created",
    StructuredArguments.keyValue("person_id", person.id),
    StructuredArguments.keyValue("email", person.email),
    StructuredArguments.keyValue("operation", "create")
)

// Results in Elasticsearch:
{
  "message": "Person created",
  "person_id": 5,
  "email": "john@example.com",
  "operation": "create"
}
```

### MDC (Mapped Diagnostic Context)

**Key Aspects of MDC:**
- Thread-Local Storage: MDC uses a map-based structure to store key-value pairs that are local to the current thread, ensuring thread safety.
- Enhanced Traceability: It allows developers to trace a single request's journey across complex, asynchronous, or multi-threaded applications.
- Automatic Injection: Once configured, MDC automatically includes this contextual data in every log entry, making it easy to filter logs by specific users or transactions.
- Example Usage: MDC.put("requestId", "12345"); before a process and MDC.clear();

Add request-scoped context:

```groovy
import org.slf4j.MDC

@Component
class RequestLoggingFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) {
        
        try {
            // Add request ID to all logs for this request
            MDC.put("request_id", UUID.randomUUID().toString())
            MDC.put("user_ip", request.getRemoteAddr())
            
            filterChain.doFilter(request, response)
        } finally {
            // Always clean up MDC
            MDC.clear()
        }
    }
}

// Now all logs include request_id and user_ip
log.info("Processing request")
// → {"message": "Processing request", "request_id": "abc-123", "user_ip": "192.168.1.100"}
```

---

## Logstash Pipeline Configuration

### Docker Compose Setup

```yaml
logstash:
  image: docker.elastic.co/logstash/logstash:7.10.2
  container_name: logstash
  environment:
    - LS_JAVA_OPTS=-Xms256m -Xmx256m
  ports:
    - "4560:4560"  # TCP input
    - "5044:5044"  # Beats input (future use)
  volumes:
    - ./logstash-config:/usr/share/logstash/pipeline
  depends_on:
    elasticsearch:
      condition: service_healthy
```

### Pipeline Configuration

#### logstash-config/logstash.conf

```ruby
input {
  # TCP input for receiving JSON logs from applications
  tcp {
    port => 4560
    codec => json_lines
    
    # Connection settings
    tcp_keep_alive => true
    ssl_enable => false  # Enable for production
  }
  
  # Optional: Beats input for filebeat/metricbeat
  beats {
    port => 5044
  }
}

filter {
  # Add environment tag
  mutate {
    add_field => { 
      "environment" => "docker"
      "pipeline_version" => "1.0"
    }
  }
  
  # Parse the timestamp
  date {
    match => [ "@timestamp", "ISO8601" ]
    target => "@timestamp"
  }
  
  # Extract service instance from application_name if present
  if [application_name] == "person-service-client" {
    mutate {
      add_field => { "service_type" => "backend" }
    }
  } else if [application_name] == "person-front" {
    mutate {
      add_field => { "service_type" => "frontend" }
    }
  }
  
  # Parse exception stack traces
  if [level] == "ERROR" and [stack_trace] {
    mutate {
      add_tag => [ "has_exception" ]
    }
  }
  
  # Add geo location for IP addresses (if needed)
  # geoip {
  #   source => "user_ip"
  #   target => "geoip"
  # }
}

output {
  # Send to Elasticsearch
  elasticsearch {
    hosts => ["elasticsearch:9200"]
    
    # Daily indices
    index => "logstash-%{+YYYY.MM.dd}"
    
    # Document ID (optional, auto-generated if omitted)
    # document_id => "%{[@metadata][fingerprint]}"
    
    # Template settings (optional)
    manage_template => true
    template_name => "logstash"
    template_overwrite => false
  }
  
  # Optional: Output to console for debugging
  # stdout {
  #   codec => rubydebug
  # }
}
```

### Filter Plugins

#### Mutate Filter

Add, remove, or modify fields:

```ruby
filter {
  mutate {
    # Add new fields
    add_field => { 
      "datacenter" => "us-west-2"
      "team" => "backend"
    }
    
    # Remove fields
    remove_field => [ "field_to_remove" ]
    
    # Rename fields
    rename => { "old_field" => "new_field" }
    
    # Convert field type
    convert => { "response_time" => "integer" }
    
    # Add tags
    add_tag => [ "processed" ]
    
    # Replace field value
    replace => { "message" => "REDACTED" }
  }
}
```

#### Grok Filter

Parse unstructured log messages:

```ruby
filter {
  grok {
    match => { 
      "message" => "%{TIMESTAMP_ISO8601:timestamp} %{LOGLEVEL:level} %{GREEDYDATA:log_message}" 
    }
  }
}
```

#### Date Filter

Parse timestamps:

```ruby
filter {
  date {
    match => [ "timestamp", "ISO8601", "yyyy-MM-dd HH:mm:ss" ]
    target => "@timestamp"
    timezone => "UTC"
  }
}
```

#### Drop Filter

Discard certain log entries:

```ruby
filter {
  # Drop debug logs in production
  if [level] == "DEBUG" {
    drop { }
  }
  
  # Drop health check logs to reduce noise
  if [message] =~ /actuator\/health/ {
    drop { }
  }
}
```

### Testing Pipeline Configuration

```bash
# Check configuration syntax
docker exec logstash /usr/share/logstash/bin/logstash \
  --config.test_and_exit \
  -f /usr/share/logstash/pipeline/logstash.conf

# Run with verbose output
docker exec logstash /usr/share/logstash/bin/logstash \
  --log.level=debug \
  -f /usr/share/logstash/pipeline/logstash.conf
```

---

## Kibana Setup & Usage

### Initial Setup

#### 1. Access Kibana

Open browser: **http://localhost:5601**

First time, you'll see the Kibana welcome screen.

#### 2. Create Index Pattern

**Step 1**: Navigate to Management
- Click hamburger menu (☰) → Management → Stack Management

**Step 2**: Go to Index Patterns
- Click "Index Patterns" under Kibana section

**Step 3**: Create Index Pattern
- Click "Create index pattern"
- Enter pattern: `logstash-*`
- This matches: `logstash-2026.04.18`, `logstash-2026.04.19`, etc.

**Step 4**: Select Time Field
- Choose `@timestamp` from dropdown
- Click "Create index pattern"

**Step 5**: Verify
- You should see list of fields (message, level, application_name, etc.)

#### 3. Configure Default Columns

Go to Discover view and customize columns:
- Click "+ Add" → Select: `@timestamp`, `level`, `application_name`, `message`
- Click "Save" → "Save current query"

### Using Discover View

#### Basic Search

**Open Discover**:
- Click hamburger menu (☰) → Discover

**Select Time Range**:
- Click time picker (top right)
- Choose: Last 15 minutes, Last hour, Last 24 hours, or custom range

**Search Box**: Uses KQL (Kibana Query Language)

```
# Find all errors
level: ERROR

# Find logs from specific service
application_name: "person-service-client"

# Combine conditions (AND)
level: ERROR AND application_name: "person-service-client"

# Combine conditions (OR)
level: ERROR OR level: WARN

# Wildcard search
message: *exception*

# Phrase search
message: "Person created successfully"

# Field exists
_exists_: stack_trace

# Field does not exist
NOT _exists_: stack_trace

# Numeric comparison
level_value >= 30000

# Date range
@timestamp >= "2026-04-20T00:00:00" AND @timestamp < "2026-04-21T00:00:00"
```

#### Example Queries

```
# All errors from service-1
level: ERROR AND message: *service-1*

# Find specific person operations
message: "Person created" OR message: "Person updated" OR message: "Person deleted"

# Find database errors
message: *SQLException* OR message: *database*

# Find slow queries (if logged)
message: *slow* OR message: *timeout*

# Find validation errors
message: *validation* OR message: *constraint*

# Find specific user actions (if request_id in MDC)
request_id: "abc-123-def-456"

# Find logs from specific thread
thread_name: "http-nio-8081-exec-5"

# Find logs from specific time range with error
@timestamp >= "2026-04-20T19:00:00" AND level: ERROR
```

#### Filtering

**Add Filter**:
1. Click "+ Add filter"
2. Select field: `level`
3. Select operator: `is`
4. Select value: `ERROR`
5. Click "Save"

**Filter from Field Value**:
- Hover over a field value in a log entry
- Click (+) to filter for this value
- Click (-) to filter out this value

#### Time Range Selection

**Quick Ranges**:
- Last 15 minutes
- Last 1 hour
- Last 24 hours
- Last 7 days
- Today
- This week

**Relative**:
- Last X minutes/hours/days
- Next X minutes/hours/days (for future timestamps)

**Absolute**:
- Specific start and end date/time
- Format: `YYYY-MM-DD HH:mm:ss`

**Auto-refresh**:
- Enable auto-refresh (top right)
- Set interval: 5s, 10s, 30s, 1m, etc.

#### Viewing Log Details

Click on any log entry to expand:

```json
{
  "@timestamp": "2026-04-20T19:45:23.123Z",
  "@version": "1",
  "message": "Person created: Person{id=5, name='John', lastname='Doe'}",
  "logger_name": "io.github.joxebus.controller.PersonController",
  "thread_name": "http-nio-8081-exec-5",
  "level": "INFO",
  "level_value": 20000,
  "application_name": "person-service-client",
  "environment": "docker",
  "service_type": "backend"
}
```

**Actions**:
- View surrounding documents (context view)
- Filter for/out field values
- Copy field value
- View JSON source

#### Saving Searches

1. Build your query
2. Click "Save" (top right)
3. Enter name: "Backend Errors"
4. Click "Save"

**Loading Saved Searches**:
- Click "Open" (top right)
- Select from list

### Kibana Query Language (KQL) Reference

#### Syntax

```
field: value                    # Exact match
field: "multiple words"         # Phrase
field: value*                   # Wildcard (prefix)
field: *value*                  # Wildcard (contains)
field: value AND other: value2  # AND condition
field: value OR other: value2   # OR condition
NOT field: value                # Negation
(field: value)                  # Grouping
field >= 100                    # Numeric comparison
field: [100 TO 200]             # Range
_exists_: field                 # Field exists
```

#### Operators

| Operator | Description | Example |
|----------|-------------|---------|
| `:` | Equals | `level: ERROR` |
| `>=` | Greater than or equal | `level_value >= 40000` |
| `<=` | Less than or equal | `response_time <= 100` |
| `>` | Greater than | `retry_count > 3` |
| `<` | Less than | `response_time < 50` |
| `AND` | Both conditions | `level: ERROR AND application_name: "person-service-client"` |
| `OR` | Either condition | `level: ERROR OR level: WARN` |
| `NOT` | Negate condition | `NOT level: DEBUG` |

#### Examples by Use Case

**Find Exceptions**:
```
_exists_: stack_trace
```

**Find Specific Exception Type**:
```
stack_trace: *NullPointerException*
```

**Find Errors in Last Hour**:
```
level: ERROR AND @timestamp >= now-1h
```

**Find Slow Responses** (if logged):
```
message: *duration* AND duration > 1000
```

**Find Specific User Actions** (if user_id in logs):
```
user_id: "user123" AND message: *created*
```

**Exclude Health Checks**:
```
NOT message: *actuator/health*
```

---

## Creating Visualizations

### Visualization Types

| Type | Use Case | Example |
|------|----------|---------|
| **Line Chart** | Trends over time | Log volume per hour |
| **Area Chart** | Stacked trends | Log levels distribution over time |
| **Bar Chart** | Compare categories | Error count by service |
| **Pie Chart** | Proportions | Percentage of log levels |
| **Data Table** | Raw aggregated data | Top 10 error messages |
| **Metric** | Single number | Total error count |
| **Tag Cloud** | Word frequency | Most common log messages |
| **Heat Map** | Patterns in matrix | Errors by hour and service |

### Creating a Visualization

#### Example 1: Error Rate Over Time (Line Chart)

**Step 1**: Create Visualization
- Go to Visualize → Create visualization
- Select "Line" chart

**Step 2**: Choose Index Pattern
- Select `logstash-*`

**Step 3**: Configure Y-Axis
- Metrics → Count
- Label: "Error Count"

**Step 4**: Configure X-Axis
- Buckets → X-Axis
- Aggregation: Date Histogram
- Field: @timestamp
- Interval: Auto (or 1h, 5m, etc.)

**Step 5**: Add Filter
- Click "Add filter"
- Field: `level`
- Value: `ERROR`

**Step 6**: Save
- Click "Save" → Enter name: "Error Rate Over Time"

#### Example 2: Log Levels Distribution (Pie Chart)

**Step 1**: Create Pie Visualization
- Visualize → Create → Pie

**Step 2**: Configure Slice Size
- Metrics → Count

**Step 3**: Configure Slices
- Buckets → Split slices
- Aggregation: Terms
- Field: level.keyword
- Order By: Metric: Count
- Order: Descending
- Size: 5

**Step 4**: Save
- Name: "Log Levels Distribution"

#### Example 3: Top Error Messages (Data Table)

**Step 1**: Create Data Table
- Visualize → Create → Data Table

**Step 2**: Configure Metrics
- Metrics → Count
- Label: "Occurrences"

**Step 3**: Configure Rows
- Buckets → Split rows
- Aggregation: Terms
- Field: message.keyword
- Order By: Metric: Count
- Order: Descending
- Size: 10

**Step 4**: Add Filter
- level: ERROR

**Step 5**: Save
- Name: "Top 10 Error Messages"

#### Example 4: Service Activity (Metric)

**Step 1**: Create Metric Visualization
- Visualize → Create → Metric

**Step 2**: Configure Metric
- Aggregation: Count
- Custom Label: "Total Logs (Last Hour)"

**Step 3**: Set Time Range
- Last 1 hour

**Step 4**: Save
- Name: "Log Volume - Last Hour"

### Creating a Dashboard

**Step 1**: Create Dashboard
- Click Dashboard → Create dashboard

**Step 2**: Add Visualizations
- Click "Add"
- Select saved visualizations:
  - Error Rate Over Time
  - Log Levels Distribution
  - Top 10 Error Messages
  - Log Volume - Last Hour

**Step 3**: Arrange Layout
- Drag and resize panels
- Organize logically (overview metrics at top, details below)

**Step 4**: Set Time Range
- Use dashboard-level time picker
- All visualizations sync to same time range

**Step 5**: Save Dashboard
- Click "Save"
- Name: "Application Monitoring Dashboard"

**Step 6**: Share Dashboard
- Click "Share" → Get shareable link or embed code

### Advanced: Creating Custom Visualizations

#### TSVB (Time Series Visual Builder)

For complex time series visualizations:

```
Use Case: Compare error rates across all 3 backend services

1. Create TSVB visualization
2. Add Series 1: 
   - Aggregation: Count
   - Group by: Terms (application_name.keyword)
   - Filter: level: ERROR
3. Panel options:
   - Chart type: Line
   - Y-axis: Count
   - X-axis: Time
4. Save: "Error Comparison by Service"
```

#### Vega/Vega-Lite

For custom D3.js-like visualizations (advanced):

```json
{
  "$schema": "https://vega.github.io/schema/vega-lite/v5.json",
  "data": {
    "url": {
      "index": "logstash-*",
      "body": {
        "aggs": {
          "by_hour": {
            "date_histogram": {
              "field": "@timestamp",
              "interval": "1h"
            }
          }
        }
      }
    }
  },
  "mark": "bar",
  "encoding": {
    "x": {"field": "key_as_string", "type": "temporal"},
    "y": {"field": "doc_count", "type": "quantitative"}
  }
}
```

---

## Monitoring Application Health via Logs

### Key Patterns to Monitor

#### 1. Error Rate Spike

**Normal**: 0-5 errors per minute  
**Warning**: > 10 errors per minute  
**Critical**: > 50 errors per minute

**Query**:
```
level: ERROR
```

**Visualization**: Line chart with threshold markers

#### 2. Specific Exception Types

**Watch for**:
- `NullPointerException` - Logic bug
- `SQLException` - Database connectivity or query issues
- `ConstraintViolationException` - Validation failures
- `TimeoutException` - External dependency issues

**Query**:
```
stack_trace: *NullPointerException*
```

#### 3. Startup/Shutdown Events

**Monitor**:
- Application started
- Application stopped
- Graceful vs ungraceful shutdown

**Query**:
```
message: "Started BackApplication" OR message: "Stopped BackApplication"
```

#### 4. Health Check Failures

**Query**:
```
message: *actuator/health* AND (level: WARN OR level: ERROR)
```

#### 5. Database Connection Issues

**Query**:
```
message: (*connection* OR *database*) AND level: ERROR
```

#### 6. Slow Operations

If logging response times:

**Query**:
```
message: *duration* AND duration > 1000
```

### Setting Up Alerts (Kibana Alerting)

#### Example: Alert on High Error Rate

**Step 1**: Create Threshold Alert
- Management → Stack Management → Alerts → Create alert

**Step 2**: Configure Trigger
- Name: "High Error Rate"
- Check every: 1 minute
- Index: `logstash-*`

**Step 3**: Define Condition
```
WHEN count()
GROUPED OVER all documents
IS ABOVE 50
FOR THE LAST 5 minutes
```

**Step 4**: Add Filter
```
level: ERROR
```

**Step 5**: Configure Actions
- Action: Email
- To: ops-team@example.com
- Subject: "ALERT: High Error Rate Detected"
- Body:
  ```
  {{context.date}}
  Error count: {{context.value}}
  
  View in Kibana:
  http://localhost:5601/app/discover
  ```

**Step 6**: Save Alert

#### Example: Alert on Specific Exception

**Condition**:
```
WHEN count()
WHERE stack_trace: *OutOfMemoryError*
IS ABOVE 0
FOR THE LAST 1 minute
```

**Action**: Send to Slack channel #incidents

### Correlation Patterns

#### Request Flow Tracing

If using MDC with `request_id`:

**Query**:
```
request_id: "abc-123-def-456"
```

**Result**: See all log entries for single request across all services

**Timeline**:
```
10:30:45.123 [person-front]          Received request for /people
10:30:45.145 [person-service-client] Processing GET /people
10:30:45.167 [person-service-client] Database query executed
10:30:45.189 [person-service-client] Returning 2 results
10:30:45.201 [person-front]          Rendering response
```

#### Error to Root Cause

Follow logs backward from error:

1. Find error in Kibana
2. Note timestamp: `2026-04-20T19:45:23.123Z`
3. Query logs 1 minute before error:
   ```
   @timestamp >= "2026-04-20T19:44:23" AND @timestamp <= "2026-04-20T19:45:23"
   ```
4. Look for WARNings or patterns leading to error

---

## Log Best Practices

### Application Side

#### 1. Use Appropriate Log Levels

```groovy
// ✅ INFO: Business events
log.info("Order placed: orderId=${order.id}, amount=${order.total}")

// ✅ DEBUG: Development/troubleshooting
log.debug("Entering method: calculateTotal with items=${items.size()}")

// ✅ WARN: Recoverable issues
log.warn("Retry attempt ${attempt} for sending email to ${user.email}")

// ✅ ERROR: Serious problems
log.error("Failed to process payment for order ${order.id}", exception)
```

#### 2. Include Context

```groovy
// ❌ Bad: No context
log.error("Save failed")

// ✅ Good: Full context
log.error("Failed to save person: id=${person.id}, email=${person.email}", exception)
```

#### 3. Use Parameterized Logging

```groovy
// ❌ Bad: String concatenation (always evaluated)
log.debug("Person details: " + person.toString())

// ✅ Good: Parameterized (only evaluated if DEBUG enabled)
log.debug("Person details: {}", person)
```

#### 4. Don't Log Sensitive Data

```groovy
// ❌ Never log:
// - Passwords
// - Credit card numbers
// - Social security numbers
// - API keys/tokens
// - Personal health information

// ❌ Bad
log.info("User login: username=${username}, password=${password}")

// ✅ Good
log.info("User login: username=${username}")
```

#### 5. Use Structured Logging

```groovy
import net.logstash.logback.argument.StructuredArguments.*

// ✅ Structured fields for better querying
log.info("Payment processed",
    keyValue("order_id", order.id),
    keyValue("amount", order.total),
    keyValue("currency", "USD"),
    keyValue("payment_method", "credit_card")
)
```

#### 6. Avoid Logging in Loops

```groovy
// ❌ Bad: Log spam
persons.each { person ->
    log.debug("Processing person: ${person.id}")  // 1000 log lines!
}

// ✅ Good: Summary logging
log.info("Processing ${persons.size()} persons")
// ... process ...
log.info("Completed processing: successful=${successCount}, failed=${failCount}")
```

### Infrastructure Side

#### 1. Set Retention Policies

```bash
# Index Lifecycle Management (ILM) policy

# Hot phase (0-3 days): Full query performance
# Warm phase (3-7 days): Reduced replicas
# Cold phase (7-30 days): Searchable snapshots
# Delete phase (30+ days): Delete indices
```

**Create ILM Policy**:
```json
PUT _ilm/policy/logstash-policy
{
  "policy": {
    "phases": {
      "hot": {
        "actions": {
          "rollover": {
            "max_size": "50GB",
            "max_age": "1d"
          }
        }
      },
      "warm": {
        "min_age": "3d",
        "actions": {
          "allocate": {
            "number_of_replicas": 0
          }
        }
      },
      "delete": {
        "min_age": "30d",
        "actions": {
          "delete": {}
        }
      }
    }
  }
}
```

#### 2. Monitor Elasticsearch Cluster Health

```bash
# Daily health check
curl http://localhost:9200/_cluster/health?pretty

# Monitor disk space
curl http://localhost:9200/_cat/allocation?v

# Check slow queries
curl http://localhost:9200/_cat/indices?v&s=search.query_time_in_millis:desc
```

#### 3. Configure Log Rotation

Logstash automatically creates daily indices:
```
logstash-2026.04.18  (50 GB, 10M docs)
logstash-2026.04.19  (48 GB, 9.5M docs)
logstash-2026.04.20  (current)
```

#### 4. Backup Important Indices

```bash
# Create snapshot repository
PUT /_snapshot/backup_repo
{
  "type": "fs",
  "settings": {
    "location": "/backup/elasticsearch"
  }
}

# Create snapshot
PUT /_snapshot/backup_repo/snapshot_2026-04-20
{
  "indices": "logstash-2026.04.*",
  "include_global_state": false
}
```

#### 5. Secure Elasticsearch

```yaml
# elasticsearch.yml
xpack.security.enabled: true
xpack.security.transport.ssl.enabled: true
xpack.security.http.ssl.enabled: true
```

---

## Troubleshooting ELK Stack

### Issue 1: Logs Not Appearing in Kibana

**Symptoms**:
- Application logs to console
- But nothing in Kibana

**Diagnostic Steps**:

1. **Check Logstash connection**:
   ```bash
   docker compose logs service-1 | grep "logstash"
   
   # Look for connection errors:
   # "Connection refused"
   # "Connection timeout"
   ```

2. **Test Logstash directly**:
   ```bash
   echo '{"message":"test","level":"INFO"}' | nc localhost 4560
   ```

3. **Check Logstash logs**:
   ```bash
   docker compose logs logstash | tail -50
   
   # Look for:
   # Pipeline started successfully
   # Connection errors to Elasticsearch
   ```

4. **Check Elasticsearch**:
   ```bash
   curl http://localhost:9200/_cat/indices?v
   
   # Should see: logstash-2026.04.20
   ```

5. **Verify index pattern in Kibana**:
   - Management → Index Patterns
   - Check if `logstash-*` exists
   - Refresh field list if needed

**Common Solutions**:

- **Logstash not started**: `docker compose up -d logstash`
- **Wrong host in log-config.xml**: Update `<destination>logstash:4560</destination>`
- **Firewall blocking**: Check Docker network connectivity
- **Index pattern mismatch**: Recreate index pattern in Kibana

### Issue 2: Elasticsearch Cluster Yellow/Red Status

**Symptoms**:
```bash
curl http://localhost:9200/_cluster/health
{
  "status": "yellow",  # or "red"
  "unassigned_shards": 5
}
```

**Causes**:

**Yellow Status**: Replica shards not assigned (normal for single-node)
**Red Status**: Primary shards not assigned (data loss risk!)

**Solutions**:

**For Yellow (Single Node)**:
```bash
# Reduce replica count to 0
PUT logstash-*/_settings
{
  "number_of_replicas": 0
}
```

**For Red**:
```bash
# Check unassigned shards
curl http://localhost:9200/_cat/shards?v | grep UNASSIGNED

# Reallocate if possible
POST /_cluster/reroute?retry_failed=true

# Last resort: Delete corrupted index
DELETE /logstash-2026.04.20
```

### Issue 3: Kibana Not Loading

**Symptoms**:
- Browser shows "Kibana server is not ready yet"
- White screen
- 502 Bad Gateway

**Diagnostic Steps**:

1. **Check Kibana container**:
   ```bash
   docker compose ps kibana
   
   # Should show: Up (healthy)
   ```

2. **Check Kibana logs**:
   ```bash
   docker compose logs kibana | tail -100
   
   # Look for:
   # "Kibana is now available"
   # Connection errors to Elasticsearch
   ```

3. **Check Elasticsearch connection**:
   ```bash
   curl http://localhost:9200/_cluster/health
   ```

**Solutions**:

- **Elasticsearch not ready**: Wait for Elasticsearch to start (yellow status OK)
- **Memory issues**: Increase Docker memory to 4GB+
- **Port conflict**: Check if port 5601 is available
- **Restart Kibana**: `docker compose restart kibana`

### Issue 4: High Disk Usage

**Symptoms**:
- Elasticsearch disk full warnings
- Cannot create new indices

**Check Disk Usage**:
```bash
# By index
curl http://localhost:9200/_cat/indices?v&h=index,store.size&s=store.size:desc

# By node
curl http://localhost:9200/_cat/allocation?v
```

**Solutions**:

1. **Delete old indices**:
   ```bash
   # Delete indices older than 30 days
   DELETE /logstash-2026.03.*
   ```

2. **Force merge old indices**:
   ```bash
   POST /logstash-2026.04.10/_forcemerge?max_num_segments=1
   ```

3. **Configure ILM** (see Best Practices section)

4. **Increase disk space** (long-term solution)

### Issue 5: Slow Queries

**Symptoms**:
- Kibana searches take > 10 seconds
- High CPU usage on Elasticsearch

**Diagnostic**:
```bash
# Check slow queries
GET /logstash-*/_search
{
  "profile": true,
  "query": {
    "match": { "message": "error" }
  }
}
```

**Solutions**:

1. **Reduce time range**: Search last 24h instead of last 30 days
2. **Use more specific filters**: Add filters before full-text search
3. **Increase heap size**: 
   ```yaml
   environment:
     - ES_JAVA_OPTS=-Xms1g -Xmx1g
   ```
4. **Add more nodes**: Scale horizontally

### Issue 6: Logstash Pipeline Errors

**Symptoms**:
```
[2026-04-20T19:45:23,456][ERROR][logstash.filters.grok] Error processing event
```

**Check Pipeline**:
```bash
# Test configuration
docker exec logstash /usr/share/logstash/bin/logstash \
  --config.test_and_exit \
  -f /usr/share/logstash/pipeline/logstash.conf

# Check syntax errors
# Check filter logic
# Verify field names
```

**Common Errors**:

- Typo in field name
- Invalid regex in grok pattern
- Missing filter plugin

---

## Performance Considerations

### Elasticsearch

#### JVM Heap Sizing

```yaml
elasticsearch:
  environment:
    # Rule: Set to 50% of available RAM, max 31GB
    - ES_JAVA_OPTS=-Xms512m -Xmx512m  # Development
    # - ES_JAVA_OPTS=-Xms2g -Xmx2g    # Production (8GB RAM)
    # - ES_JAVA_OPTS=-Xms16g -Xmx16g  # Production (32GB RAM)
```

**Guidelines**:
- Never exceed 31GB (compressed pointers break above this)
- Set Xms = Xmx (avoid resizing)
- Leave 50% RAM for filesystem cache

#### Sharding Strategy

```
Small daily volume (< 10GB/day):  1 primary shard
Medium volume (10-50GB/day):      2-3 primary shards
Large volume (> 50GB/day):        5+ primary shards
```

**Configure in index template**:
```json
PUT _index_template/logstash
{
  "index_patterns": ["logstash-*"],
  "template": {
    "settings": {
      "number_of_shards": 1,
      "number_of_replicas": 0,
      "refresh_interval": "30s"
    }
  }
}
```

#### Query Optimization

```
Slow ❌: Full-text search across all fields
message: *error*

Fast ✅: Exact field match
level: ERROR

Slow ❌: Wildcard at beginning
message: *Exception

Fast ✅: Wildcard at end
message: Exception*

Slow ❌: Large time range
@timestamp >= now-30d

Fast ✅: Smaller time range
@timestamp >= now-1d
```

### Logstash

#### Pipeline Workers

```yaml
logstash:
  environment:
    - PIPELINE_WORKERS=2  # Default: CPU cores
    - PIPELINE_BATCH_SIZE=125  # Events per batch
    - PIPELINE_BATCH_DELAY=50  # Wait time (ms)
```

**Tuning**:
- More workers = higher throughput, more memory
- Larger batch = better throughput, higher latency
- Adjust based on load and resources

#### Memory Settings

```yaml
logstash:
  environment:
    - LS_JAVA_OPTS=-Xms256m -Xmx256m  # Development
    # - LS_JAVA_OPTS=-Xms1g -Xmx1g    # Production
```

### Application Side

#### Async Logging

```xml
<appender name="ASYNC_LOGSTASH" class="ch.qos.logback.classic.AsyncAppender">
  <appender-ref ref="LOGSTASH"/>
  <queueSize>512</queueSize>        <!-- Buffer size -->
  <discardingThreshold>0</discardingThreshold>  <!-- Don't discard -->
  <includeCallerData>false</includeCallerData>  <!-- Faster -->
  <neverBlock>false</neverBlock>    <!-- Block if queue full -->
</appender>
```

**Trade-offs**:
- Async = faster app, risk of log loss if crash
- Sync = slower app, guaranteed log delivery

#### Log Sampling

For very high volume:

```groovy
// Sample 10% of debug logs
if (Math.random() < 0.1) {
    log.debug("Detailed debug info")
}

// Or use sampling appender
```

#### Minimize Log Volume

```groovy
// ❌ Don't log in tight loops
persons.each { log.debug("Processing ${it}") }  // 1000 logs

// ✅ Log summary instead
log.info("Processing ${persons.size()} persons")
```

---

## Summary

✅ **ELK Stack provides**:
- Centralized log aggregation from all services
- Full-text search across all logs
- Real-time visualization and dashboards
- Historical analysis and trend detection
- Alert capabilities for proactive monitoring

✅ **Key takeaways**:
- Configure logstash-logback-encoder for JSON logging
- Send logs to Logstash via TCP port 4560
- Logstash processes and forwards to Elasticsearch
- Kibana provides web UI for search and visualization
- Use structured logging for better querying
- Set up index lifecycle management for retention
- Monitor Elasticsearch cluster health

✅ **Production checklist**:
- [ ] Configure authentication (X-Pack Security)
- [ ] Set up TLS/SSL encryption
- [ ] Configure index lifecycle management
- [ ] Set up automated backups
- [ ] Monitor disk space usage
- [ ] Create alerting rules
- [ ] Document log retention policy
- [ ] Train team on Kibana usage
- [ ] Set up log sampling if needed
- [ ] Configure proper heap sizes

---

## Additional Resources

- [Elasticsearch Documentation](https://www.elastic.co/guide/en/elasticsearch/reference/current/index.html)
- [Logstash Documentation](https://www.elastic.co/guide/en/logstash/current/index.html)
- [Kibana Documentation](https://www.elastic.co/guide/en/kibana/current/index.html)
- [Logstash Logback Encoder](https://github.com/logfellow/logstash-logback-encoder)
- [Kibana Query Language (KQL)](https://www.elastic.co/guide/en/kibana/current/kuery-query.html)
- [Project README](../README.md)
- [Consul Service Discovery Guide](consul-service-discovery.md)
- [Docker Commands Reference](docker-commands.md)

---

**Last Updated**: April 20, 2026  
**Version**: 1.0  
**Feedback**: Please report issues or suggest improvements via GitHub issues.
