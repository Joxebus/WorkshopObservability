# Docker Commands Reference

**Project:** Spring Boot Microservices with Consul  
**Last Updated:** 2026-04-20

This document contains all Docker and Docker Compose commands used for building, deploying, and managing the application containers.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Building Images](#building-images)
- [Starting Services](#starting-services)
- [Stopping Services](#stopping-services)
- [Viewing Logs](#viewing-logs)
- [Container Management](#container-management)
- [Consul Operations](#consul-operations)
- [Database Operations](#database-operations)
- [Troubleshooting](#troubleshooting)
- [Cleanup](#cleanup)

## Prerequisites

### Check Docker Status
```bash
# Check if Docker daemon is running
docker ps

# Check Docker version
docker --version
docker compose version
```

### Environment Setup
```bash
# Ensure Java 21 is active (using SDKMAN)
sdk use java 21.0.6-amzn
```

## Building Images

### Build All Services
```bash
# Clean build all Maven modules first
mvn clean package -DskipTests

# Build all Docker images defined in docker-compose.yml
docker compose build
```

### Build Specific Services
```bash
# Build only frontend and backend services
docker compose build front-1 service-1

# Build with no cache (force rebuild)
docker compose build --no-cache front-1 service-1
```

### Build Individual Service
```bash
# Build only the frontend
docker compose build front-1

# Build only the backend
docker compose build service-1
```

## Starting Services

### Start All Services
```bash
# Start all services defined in docker-compose.yml
docker compose up -d

# Start with build (build and start)
docker compose up -d --build
```

### Start Infrastructure Services
```bash
# Start only Consul
docker compose up -d consul

# Start database
docker compose up -d mysql

# Start ELK stack
docker compose up -d elasticsearch logstash kibana
```

### Start Application Services
```bash
# Start backend service
docker compose up -d service-1

# Start frontend service
docker compose up -d front-1

# Start all backend instances (service-1, service-2, service-3)
docker compose up -d service-1 service-2 service-3
```

### Force Recreate Containers
```bash
# Recreate containers even if configuration hasn't changed
docker compose up -d --force-recreate front-1 service-1

# Rebuild and recreate
docker compose up -d --build --force-recreate front-1
```

### Start in Foreground (with logs)
```bash
# Start and follow logs (useful for debugging)
docker compose up front-1 service-1

# Start specific services in foreground
docker compose up consul mysql
```

## Stopping Services

### Stop All Services
```bash
# Stop all running containers
docker compose stop

# Stop and remove containers, networks
docker compose down
```

### Stop Specific Services
```bash
# Stop frontend
docker compose stop front-1

# Stop backend
docker compose stop service-1

# Stop multiple services
docker compose stop front-1 service-1
```

### Restart Services
```bash
# Restart all services
docker compose restart

# Restart specific service
docker compose restart front-1

# Restart multiple services
docker compose restart front-1 service-1
```

## Viewing Logs

### View All Logs
```bash
# View logs from all containers
docker compose logs

# Follow logs in real-time
docker compose logs -f
```

### View Service-Specific Logs
```bash
# View frontend logs
docker compose logs front-1

# View backend logs
docker compose logs service-1

# View Consul logs
docker compose logs consul
```

### Follow Logs in Real-Time
```bash
# Follow frontend logs
docker compose logs -f front-1

# Follow backend logs
docker compose logs -f service-1

# Follow multiple services
docker compose logs -f front-1 service-1
```

### Filter Logs
```bash
# View last 50 lines
docker compose logs --tail=50 front-1

# View last 20 lines from service
docker compose logs --tail=20 service-1

# View logs since last 5 minutes
docker compose logs --since 5m front-1

# View logs since last 2 minutes
docker compose logs --since 2m service-1
```

### Search Logs
```bash
# Search for errors in frontend
docker compose logs front-1 | grep ERROR

# Search for warnings
docker compose logs front-1 | grep WARN

# Search for specific text
docker compose logs front-1 | grep "Started FrontApplication"

# Search for deprecated warnings (Thymeleaf)
docker compose logs front-1 | grep "Deprecated unwrapped fragment"

# Get last 10 error lines
docker compose logs front-1 | grep ERROR | tail -10
```

## Container Management

### List Running Containers
```bash
# List all running containers
docker compose ps

# List with custom format
docker compose ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}"

# List all containers (including stopped)
docker ps -a
```

### Check Container Health
```bash
# Check if elasticsearch is healthy
docker compose ps elasticsearch

# View health status
docker inspect --format='{{json .State.Health}}' elasticsearch | jq
```

### Execute Commands in Containers
```bash
# Open shell in frontend container
docker exec -it front sh

# Open shell in backend container
docker exec -it service-1 sh

# Execute single command
docker exec front curl -s http://localhost:8080/actuator/health

# Test connectivity from frontend to backend
docker exec front curl -s http://service-1:8081/people
```

### Test DNS Resolution
```bash
# Check if service name resolves
docker exec front nslookup service-1

# Test Consul connectivity
docker exec front curl -s http://consul:8500/v1/catalog/services
```

## Consul Operations

### Check Consul Status
```bash
# Check Consul health
curl http://localhost:8500/v1/status/leader

# List all services
curl http://localhost:8500/v1/catalog/services | jq

# Get service instances
curl http://localhost:8500/v1/catalog/service/person-service-client | jq
```

### Service Health Checks
```bash
# Check backend service health in Consul
curl http://localhost:8500/v1/health/service/person-service-client | jq

# Check specific health status
curl http://localhost:8500/v1/health/service/person-service-client | \
  jq '.[].Checks[] | select(.ServiceName == "person-service-client") | {Status, Output}'

# Get service registration details
curl http://localhost:8500/v1/health/service/person-service-client | \
  jq '.[0].Service | {ID, Address, Port}'
```

### Consul UI
```bash
# Access Consul UI in browser
open http://localhost:8500/ui/
```

## Database Operations

### MySQL Container
```bash
# Check MySQL logs
docker compose logs mysql

# Connect to MySQL
docker exec -it mysql mysql -u consul -p
# Password: example

# Check database
docker exec -it mysql mysql -u consul -pexample -e "SHOW DATABASES;"

# Query person table
docker exec -it mysql mysql -u consul -pexample consul-example \
  -e "SELECT * FROM person;"
```

### H2 Database (Local Development)
```bash
# H2 Console access (when service running)
open http://localhost:8081/h2-console

# Connection details:
# JDBC URL: jdbc:h2:file:~/consul-example
# User: sa
# Password: (empty)
```

## Troubleshooting

### Check Container Status
```bash
# Detailed container inspection
docker inspect service-1

# Check why container stopped
docker compose ps -a
docker compose logs service-1 --tail=50
```

### Port Conflicts
```bash
# Check if port is already in use
lsof -ti:8080
lsof -ti:8081

# Kill process using port
lsof -ti:8080 | xargs kill -9
```

### Network Issues
```bash
# List Docker networks
docker network ls

# Inspect network
docker network inspect workshoparquitecturajava_default

# Check container IP addresses
docker inspect -f '{{.Name}} - {{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' $(docker ps -aq)
```

### Container Resources
```bash
# Check container resource usage
docker stats

# Check specific container
docker stats service-1

# One-time snapshot
docker stats --no-stream
```

### View Container Processes
```bash
# List processes in container
docker top service-1

# Detailed process info
docker exec service-1 ps aux
```

## Cleanup

### Remove Stopped Containers
```bash
# Remove all stopped containers
docker compose down

# Remove containers and volumes
docker compose down -v

# Remove containers, networks, and images
docker compose down --rmi all
```

### Clean Up Images
```bash
# Remove unused images
docker image prune

# Remove all unused images
docker image prune -a

# Remove specific image
docker rmi workshoparquitecturajava-front-1
```

### Clean Up Volumes
```bash
# List volumes
docker volume ls

# Remove specific volume
docker volume rm workshoparquitecturajava_elasticsearch_data

# Remove all unused volumes
docker volume prune
```

### Complete Cleanup
```bash
# Stop and remove everything
docker compose down -v --rmi all

# Remove all containers, networks, images, and volumes
docker system prune -a --volumes
```

## Development Workflow

### Typical Development Cycle
```bash
# 1. Make code changes
# Edit files in your IDE

# 2. Rebuild Maven artifacts
mvn clean package -DskipTests

# 3. Rebuild Docker images
docker compose build front-1 service-1

# 4. Restart containers
docker compose up -d --force-recreate front-1 service-1

# 5. View logs
docker compose logs -f front-1 service-1

# 6. Test changes
curl http://localhost:8080/people
```

### Quick Restart Workflow
```bash
# If only configuration changed (no code changes)
docker compose restart front-1 service-1

# View logs to confirm startup
docker compose logs --tail=30 front-1 service-1
```

## Testing & Validation

### Health Checks
```bash
# Check all actuator endpoints
curl http://localhost:8081/actuator/health
curl http://localhost:8080/actuator/health

# Check metrics
curl http://localhost:8081/actuator/metrics | jq
```

### API Testing
```bash
# Get all persons
curl http://localhost:8081/people | jq

# Create person
curl -X POST http://localhost:8081/people \
  -H "Content-Type: application/json" \
  -d '{"name":"Test","lastname":"User","email":"test@example.com"}' | jq

# Get specific person
curl http://localhost:8081/people/1 | jq
```

### Frontend Testing
```bash
# Check homepage
curl -s http://localhost:8080/ | head -20

# Check people list
curl -s http://localhost:8080/people | grep "<td>"

# Check add person form
curl -s http://localhost:8080/people/new | head -20
```

## Multi-Instance Testing

### Start Multiple Backend Instances
```bash
# Start all three backend instances
docker compose up -d service-1 service-2 service-3

# Check all instances registered in Consul
curl http://localhost:8500/v1/health/service/person-service-client | \
  jq '.[].Service | {ID, Address, Port}'

# Count instances
curl http://localhost:8500/v1/health/service/person-service-client | jq 'length'
```

### Test Load Balancing
```bash
# Make multiple requests to frontend
for i in {1..10}; do 
  curl -s http://localhost:8080/people | grep -q "Omar" && echo "Request $i: Success"
done
```

## ELK Stack Operations

### Elasticsearch
```bash
# Check cluster health
curl http://localhost:9200/_cluster/health | jq

# List indices
curl http://localhost:9200/_cat/indices?v

# Search logs
curl http://localhost:9200/_search?pretty
```

### Logstash
```bash
# Check Logstash status
curl http://localhost:9600/_node/stats | jq

# View Logstash logs
docker compose logs logstash
```

### Kibana
```bash
# Access Kibana UI
open http://localhost:5601/app/kibana

# Check Kibana status
curl http://localhost:5601/api/status | jq
```

## Performance Monitoring

### Container Resource Usage
```bash
# Real-time stats for all containers
docker stats

# Stats for specific service
docker stats service-1

# Get memory usage
docker stats --no-stream --format "table {{.Name}}\t{{.MemUsage}}"
```

### Application Metrics
```bash
# JVM metrics
curl http://localhost:8081/actuator/metrics/jvm.memory.used | jq

# HTTP metrics
curl http://localhost:8081/actuator/metrics/http.server.requests | jq

# Database connection pool
curl http://localhost:8081/actuator/metrics/hikaricp.connections | jq
```

## Useful Aliases

Add these to your `~/.bashrc` or `~/.zshrc`:

```bash
# Docker Compose shortcuts
alias dcu='docker compose up -d'
alias dcd='docker compose down'
alias dcl='docker compose logs -f'
alias dcp='docker compose ps'
alias dcr='docker compose restart'

# Service-specific
alias dcl-front='docker compose logs -f front-1'
alias dcl-service='docker compose logs -f service-1'
alias dcr-app='docker compose restart front-1 service-1'

# Build and restart
alias dcb='mvn clean package -DskipTests && docker compose build front-1 service-1 && docker compose up -d --force-recreate front-1 service-1'
```

## Environment Variables

### Setting Environment Variables
```bash
# Temporarily for single service
SERVER_PORT=8082 docker compose up -d service-2

# Using .env file (create .env in project root)
# See docker-compose.yml for available variables:
# - SERVER_PORT
# - SERVICE_ALIAS
# - SPRING_CLOUD_CONSUL_DISCOVERY_HOSTNAME
```

## Common Issues & Solutions

### Issue: Port Already in Use
```bash
# Solution: Kill process or change port
lsof -ti:8080 | xargs kill -9
```

### Issue: Container Won't Start
```bash
# Check logs for errors
docker compose logs service-1 --tail=50

# Check container status
docker compose ps -a

# Inspect container
docker inspect service-1
```

### Issue: Can't Connect to Consul
```bash
# Verify Consul is running
docker compose ps consul

# Check Consul logs
docker compose logs consul

# Test connectivity
curl http://localhost:8500/v1/status/leader
```

### Issue: Services Not Discovering Each Other
```bash
# Check service registration
curl http://localhost:8500/v1/catalog/services

# Verify hostnames resolve
docker exec front nslookup service-1

# Check DNS from container
docker exec front cat /etc/resolv.conf
```

## Best Practices

1. **Always check logs** after starting services
2. **Use health checks** to verify services are ready
3. **Clean rebuild** when encountering issues: `docker compose down && mvn clean package && docker compose build && docker compose up -d`
4. **Monitor resources** with `docker stats`
5. **Use specific service names** instead of rebuilding everything
6. **Keep images up to date** with regular rebuilds
7. **Use `.dockerignore`** to optimize build context
8. **Tag images** appropriately for version control

## Additional Resources

- [Docker Documentation](https://docs.docker.com/)
- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [Spring Boot Docker Guide](https://spring.io/guides/topicals/spring-boot-docker/)
- [Consul Documentation](https://www.consul.io/docs)

---

**Note:** All commands assume you are in the project root directory (`/Users/joxebus/workspace/WorkshopArquitecturaJava`)
