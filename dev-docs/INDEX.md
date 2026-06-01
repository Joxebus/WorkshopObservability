# Documentation Index

**Version**: 1.1  
**Last Updated**: April 21, 2026  
**Project**: Spring Boot Microservices with Consul & ELK Stack

---

## Table of Contents

1. [Documentation Files](#documentation-files)
2. [Quick Start](#quick-start)
3. [Acronyms & Terminology](#acronyms--terminology)
4. [Related Resources](#related-resources)

---

## Documentation Files

### 1. Docker Commands Reference
**File**: [`docker-commands.md`](./docker-commands.md)  
**Size**: ~600 lines  
**Purpose**: Comprehensive Docker and Docker Compose command reference

**Contents**:
- Building and managing Docker images
- Starting, stopping, and restarting services
- Viewing and filtering logs
- Troubleshooting container issues
- Container health checks and inspections
- Volume and network management
- Consul operations via Docker
- Database operations (MySQL)
- ELK Stack management
- Best practices and useful aliases

**When to use**: First stop for any Docker-related operations, troubleshooting container issues, or learning Docker Compose commands.

---

### 2. Consul Service Discovery Guide
**File**: [`consul-service-discovery.md`](./consul-service-discovery.md)  
**Size**: ~1,490 lines  
**Purpose**: Complete guide to HashiCorp Consul integration for service discovery

**Contents**:
- **What is Consul?**: Overview, features, and why we use it
- **Architecture**: How Consul works in our microservices setup
- **Service Registration**: Spring Cloud Consul configuration and lifecycle
- **Service Discovery**: @LoadBalanced RestTemplate mechanism and client-side load balancing
- **Health Checks**: Spring Boot Actuator integration and monitoring
- **Consul UI**: Walkthrough and interpretation
- **Troubleshooting**: 6 common issues with solutions
- **Best Practices**: Service naming, health check design, graceful shutdown
- **Advanced Topics**: Multi-datacenter, service mesh, ACLs, configuration management

**Key Features**:
- 40+ code examples with real configuration
- Complete API endpoint documentation
- Curl command examples for all operations
- Troubleshooting decision trees
- Production deployment considerations

**When to use**: Understanding how services discover each other, configuring new services, troubleshooting service registration issues, or learning about distributed service registries.

---

### 3. ELK Stack Centralized Logging Guide
**File**: [`elk-stack-logging.md`](./elk-stack-logging.md)  
**Size**: ~2,040 lines  
**Purpose**: Complete guide to centralized logging with Elasticsearch, Logstash, and Kibana

**Contents**:
- **Introduction**: Centralized logging concepts and benefits
- **ELK Components**: Elasticsearch, Logstash, Kibana deep dive
- **Application Integration**: logstash-logback-encoder configuration
- **Logging Configuration**: Complete logback XML examples, custom fields, MDC usage
- **Logstash Pipeline**: Input/filter/output configuration with plugin examples
- **Kibana Setup**: Index patterns, Discover view, visualization creation
- **Kibana Query Language (KQL)**: Complete query reference with 20+ examples
- **Creating Visualizations**: Line charts, pie charts, data tables, metrics, dashboards
- **Monitoring Application Health**: Key patterns, alerting setup
- **Log Best Practices**: Application and infrastructure side recommendations
- **Troubleshooting**: 6 common issues with diagnostic steps
- **Performance Considerations**: Elasticsearch, Logstash, and application-side optimization

**Key Features**:
- Complete logging architecture flow diagrams
- Real logback configuration examples
- 20+ KQL query examples for common scenarios
- Step-by-step visualization creation
- MDC (Mapped Diagnostic Context) implementation guide

**When to use**: Setting up logging for new services, searching logs in Kibana, creating log dashboards, troubleshooting logging issues, or optimizing log performance.

---

### 4. Metrics & Monitoring Guide
**File**: [`metrics-monitoring.md`](./metrics-monitoring.md)  
**Size**: ~1,900 lines  
**Purpose**: Complete guide to application metrics, monitoring, and performance analysis

**Contents**:
- **Introduction**: Metric types (counters, gauges, histograms, summaries) and observability pillars
- **Spring Boot Actuator**: JVM, HTTP, database, Tomcat metrics with real examples
- **Custom Metrics**: Micrometer integration with code examples
- **Consul Health Metrics**: Service registry and health check monitoring
- **Database Performance**: MySQL query performance, HikariCP connection pool monitoring
- **Elasticsearch Metrics**: Cluster health, index statistics, node performance
- **Interpreting Trends**: Normal vs abnormal patterns (memory leaks, performance degradation)
- **Dashboard Design**: Grafana dashboards and custom HTML dashboard examples
- **Alerting Strategy**: 4-tier alert system (P1-P4) with runbooks
- **Metric Retention**: Short/medium/long term storage strategies
- **External Integration**: Prometheus, Grafana, CloudWatch, Datadog setup
- **Performance Baselines**: Load testing, baseline establishment, and comparison

**Key Features**:
- 75+ code examples with real curl commands
- Complete dashboard layout designs
- Alert configuration with response time SLAs
- Monitoring scripts for all infrastructure components
- Load testing scripts and baseline templates

**When to use**: Understanding application performance, setting up monitoring dashboards, creating alerts, troubleshooting performance issues, or integrating with external monitoring platforms.

---

## Quick Start

### For New Developers

1. **Start here**: [`docker-commands.md`](./docker-commands.md) - Learn how to start and manage the services
2. **Then read**: [`consul-service-discovery.md`](./consul-service-discovery.md) - Understand how services communicate
3. **Next**: [`elk-stack-logging.md`](./elk-stack-logging.md) - Learn how to search and analyze logs
4. **Finally**: [`metrics-monitoring.md`](./metrics-monitoring.md) - Set up monitoring and alerts

### For Operations/DevOps

1. **Start here**: [`docker-commands.md`](./docker-commands.md) - Master container operations
2. **Then read**: [`metrics-monitoring.md`](./metrics-monitoring.md) - Set up comprehensive monitoring
3. **Next**: [`elk-stack-logging.md`](./elk-stack-logging.md) - Configure log aggregation and retention
4. **Finally**: [`consul-service-discovery.md`](./consul-service-discovery.md) - Understand service mesh and advanced topics

### For Troubleshooting

| Issue | Documentation |
|-------|---------------|
| Service not starting | [`docker-commands.md`](./docker-commands.md) → Container Health Checks |
| Service can't find another service | [`consul-service-discovery.md`](./consul-service-discovery.md) → Troubleshooting |
| Can't find logs for a specific request | [`elk-stack-logging.md`](./elk-stack-logging.md) → Kibana Query Language |
| Performance degradation | [`metrics-monitoring.md`](./metrics-monitoring.md) → Interpreting Metric Trends |
| Database connection issues | [`metrics-monitoring.md`](./metrics-monitoring.md) → Database Performance Metrics |
| Elasticsearch cluster issues | [`metrics-monitoring.md`](./metrics-monitoring.md) → Elasticsearch Cluster Metrics |

---

## Acronyms & Terminology

### Core Technologies

**API** - Application Programming Interface  
Interface for communication between software components.

**CLI** - Command Line Interface  
Text-based interface for interacting with programs.

**CRUD** - Create, Read, Update, Delete  
Basic operations for data persistence.

**DNS** - Domain Name System  
System for resolving hostnames to IP addresses.

**HTTP** - Hypertext Transfer Protocol  
Protocol for transmitting data over the web.

**JPA** - Jakarta Persistence API  
Standard specification for object-relational mapping in Java.

**JSON** - JavaScript Object Notation  
Lightweight data interchange format.

**JVM** - Java Virtual Machine  
Runtime environment for executing Java applications.

**ORM** - Object-Relational Mapping  
Technique for converting data between incompatible type systems.

**REST** - Representational State Transfer  
Architectural style for distributed systems.

**SDK** - Software Development Kit  
Collection of tools for software development.

**SQL** - Structured Query Language  
Language for managing relational databases.

**TCP** - Transmission Control Protocol  
Connection-oriented protocol for reliable data transmission.

**URL** - Uniform Resource Locator  
Reference to a web resource (e.g., http://localhost:8080).

**YAML** - YAML Ain't Markup Language  
Human-readable data serialization format.

---

### ELK Stack

**ELK** - Elasticsearch, Logstash, Kibana  
Popular stack for centralized logging and log analysis.

**Elasticsearch** - Distributed search and analytics engine  
Document-oriented database for full-text search and log storage.

**Logstash** - Log collection and processing pipeline  
Tool for ingesting, transforming, and forwarding logs.

**Kibana** - Visualization and analytics platform  
Web interface for exploring and visualizing Elasticsearch data.

**KQL** - Kibana Query Language  
Query syntax for filtering and searching logs in Kibana.

**Index** - Collection of documents in Elasticsearch  
Equivalent to a database in relational systems.

**Shard** - Subset of an Elasticsearch index  
Allows horizontal scaling by distributing data across nodes.

**Replica** - Copy of a shard for redundancy  
Provides high availability and increases search throughput.

---

### Service Discovery

**Consul** - HashiCorp service mesh solution  
Service registry, health checking, and key-value store.

**Service Discovery** - Automatic detection of network services  
Mechanism for services to find and communicate with each other dynamically.

**Service Registry** - Database of available services  
Central repository of service instances and their locations.

**Health Check** - Automated service monitoring  
Periodic test to verify service availability and functionality.

**Load Balancing** - Distribution of traffic across instances  
Technique to optimize resource use and maximize throughput.

**@LoadBalanced** - Spring Cloud annotation  
Enables client-side load balancing for RestTemplate.

**Service Instance** - Single running copy of a service  
One deployment of a microservice application.

**Datacenter (DC)** - Isolated Consul cluster  
Physical or logical grouping of Consul nodes.

**ACL** - Access Control List  
Security feature for controlling access to Consul resources.

---

### Monitoring & Metrics

**Actuator** - Spring Boot monitoring endpoints  
Exposes application health, metrics, and operational information.

**Micrometer** - Metrics instrumentation library  
Vendor-neutral metrics facade for Java applications.

**Prometheus** - Time-series monitoring system  
Open-source monitoring and alerting toolkit.

**Grafana** - Metrics visualization platform  
Dashboard and graph builder for time-series data.

**Counter** - Monotonically increasing metric  
Tracks cumulative values (e.g., total requests).

**Gauge** - Point-in-time value metric  
Tracks current state (e.g., memory usage, active connections).

**Histogram** - Distribution of values metric  
Records observations and calculates percentiles.

**Timer** - Measures duration of events  
Specialized histogram for timing operations.

**p50/p95/p99** - Percentiles  
50th/95th/99th percentile values (e.g., p95 = 95% of requests faster than this).

**SLA** - Service Level Agreement  
Commitment between service provider and client about performance.

**SLO** - Service Level Objective  
Specific measurable target within an SLA.

**Baseline** - Normal performance benchmark  
Reference point for comparison to detect anomalies.

---

### Database & Persistence

**HikariCP** - High-performance JDBC connection pool  
Default connection pool in Spring Boot for database connections.

**Connection Pool** - Reusable database connection cache  
Maintains a pool of active connections to avoid creation overhead.

**JDBC** - Java Database Connectivity  
API for connecting Java applications to databases.

**MySQL** - Open-source relational database  
Database system used for persistent data storage.

**H2** - Embedded Java SQL database  
In-memory database used for local development and testing.

**DDL** - Data Definition Language  
SQL commands for defining schema (CREATE, ALTER, DROP).

**Transaction** - Atomic unit of database work  
Sequence of operations treated as a single unit.

---

### Docker & Containers

**Docker** - Container platform  
Tool for creating and running application containers.

**Docker Compose** - Multi-container orchestration  
Tool for defining and running multi-container applications.

**Container** - Isolated runtime environment  
Packaged application with all its dependencies.

**Image** - Container template  
Blueprint for creating containers.

**Dockerfile** - Container build instructions  
Text file with commands to assemble an image.

**Volume** - Persistent storage for containers  
Mechanism for persisting data outside container lifecycle.

**Network** - Container communication layer  
Virtual network for inter-container communication.

**Health Check** - Container availability test  
Command to verify container is running correctly.

---

### Spring Boot & Java

**Spring Boot** - Java application framework  
Opinionated framework for building production-ready applications.

**Spring Cloud** - Microservices framework  
Extensions to Spring Boot for distributed systems.

**Bean** - Spring-managed object  
Object instantiated and managed by Spring IoC container.

**Dependency Injection (DI)** - Design pattern  
Technique for achieving Inversion of Control (IoC).

**@Autowired** - Spring annotation  
Marks a dependency for automatic injection.

**@Component** - Spring annotation  
Marks a class as a Spring-managed component.

**@Service** - Spring annotation  
Specialization of @Component for service layer.

**@Repository** - Spring annotation  
Specialization of @Component for data access layer.

**@RestController** - Spring annotation  
Marks a class as a REST API controller.

**Thymeleaf** - Server-side template engine  
Used for rendering HTML views in Spring applications.

**Groovy** - JVM-based programming language  
Dynamic language used in this project alongside Java syntax.

**Maven** - Build automation tool  
Project management and build tool for Java projects.

---

### Logging

**Logback** - Logging framework  
Successor to log4j, default logging framework in Spring Boot.

**SLF4J** - Simple Logging Facade for Java  
Abstraction layer for various logging frameworks.

**MDC** - Mapped Diagnostic Context  
Thread-local context for enriching log messages.

**Appender** - Log destination  
Component that writes log events to a specific output (console, file, network).

**Logger** - Logging interface  
Object used to emit log messages.

**Log Level** - Message severity  
TRACE, DEBUG, INFO, WARN, ERROR, FATAL.

**Stack Trace** - Error call history  
Sequence of method calls leading to an exception.

---

### Performance & Troubleshooting

**GC** - Garbage Collection  
Automatic memory management in JVM.

**Heap** - JVM memory area for objects  
Memory region where Java objects are allocated.

**Thread** - Unit of program execution  
Independent path of execution within a process.

**Latency** - Response time delay  
Time between request and response.

**Throughput** - Operations per unit time  
Rate at which system processes requests (e.g., requests/second).

**Bottleneck** - Performance constraint  
Resource or operation limiting overall system performance.

**Memory Leak** - Unintentional memory retention  
Gradual memory consumption due to unreleased objects.

**Deadlock** - Resource blocking conflict  
Situation where threads wait indefinitely for each other.

---

### Alert Severity Levels

**P1** - Critical Priority  
Immediate response required (page on-call engineer).

**P2** - High Priority  
Response within 30 minutes (alert team).

**P3** - Medium Priority  
Response within 2 hours (business hours).

**P4** - Low Priority  
Monitor and address during next business day.

---

## Related Resources

### Official Documentation
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Cloud Consul](https://docs.spring.io/spring-cloud-consul/docs/current/reference/html/)
- [Consul Documentation](https://www.consul.io/docs)
- [Elasticsearch Reference](https://www.elastic.co/guide/en/elasticsearch/reference/current/index.html)
- [Logstash Documentation](https://www.elastic.co/guide/en/logstash/current/index.html)
- [Kibana Guide](https://www.elastic.co/guide/en/kibana/current/index.html)
- [Micrometer Documentation](https://micrometer.io/docs)
- [Docker Documentation](https://docs.docker.com/)

### Project Files
- [README.md](../README.md) - Project overview and quick start
- [CLAUDE.md](../CLAUDE.md) - Project guidance for Claude Code
- [docker-compose.yml](../docker-compose.yml) - Service orchestration configuration
- [pom.xml](../pom.xml) - Maven project configuration

---

## Documentation Standards

### File Format
All documentation uses **GitHub Flavored Markdown** with:
- Clear hierarchical headings (H1-H4)
- Code blocks with syntax highlighting
- Tables for structured data
- Links for cross-references

### Code Examples
- All examples are **tested and working**
- Commands include expected output
- Configuration examples show complete context
- Groovy/Java code follows project conventions

### Update Policy
- Documentation updated with each major feature
- Version numbers tracked in file headers
- Last updated dates maintained
- Breaking changes highlighted

---

## Contributing to Documentation

### Adding New Documentation
1. Use existing files as templates
2. Follow the established structure
3. Include code examples
4. Add entry to this INDEX.md
5. Update acronyms section if needed

### Updating Existing Documentation
1. Update "Last Updated" date in file header
2. Maintain consistent formatting
3. Test all code examples
4. Update cross-references if needed

### Documentation Review Checklist
- [ ] All code examples tested
- [ ] Links work correctly
- [ ] Acronyms defined
- [ ] Screenshots current
- [ ] Grammar checked
- [ ] Consistent formatting

---

**Last Updated**: April 20, 2026 
**Status**: Complete ✅
