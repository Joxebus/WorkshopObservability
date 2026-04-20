# SpringBootConsulServiceDiscovery

## Technologies

- Spring Boot Parent POM 1.5.11.RELEASE
- GORM 6.1.9.RELEASE
- Hibernate Core / Ecache 5.1.0.Final
- Tomcat JDBC 8.5.0
- Java 8
- Thymeleaf
- Consul
- ELK
- Docker

## Minimum Requirements

- Docker with 4GB of memory and 1.5 of SWAP memory
- Maven 3

## Groovy Sources

First of all the project must to know were to find the `sources` for this we need to add the next line to our *pom.xml* file:

```xml
<build>
    <sourceDirectory>src/main/groovy</sourceDirectory>
    <!-- Some other configurations -->
</build>
```

## First build the project

```
$> mvn clean package
```

## Run with docker-compose

```
$> docker compose up --build
```

- Consul UI manager: `http://localhost:8500/ui/dc1/services`
- Kibana UI manager: `http://localhost:5601/app/kibana`
- Front App: `http://localhost:8080/`
- Service-1: `http://localhost:8081/`
- Service-2: `http://localhost:8082/`
- Service-3: `http://localhost:8083/`

## MySql

- dbname: consul-example
- user: consul
- pass: example
