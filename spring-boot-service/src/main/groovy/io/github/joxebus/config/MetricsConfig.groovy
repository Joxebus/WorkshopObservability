package io.github.joxebus.config

import io.github.joxebus.repository.PersonRepository
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Centralized configuration for application metrics.
 * All Counter and Timer beans are initialized here for cleaner dependency injection.
 */
@Configuration
class MetricsConfig {

    // ===== Service Layer Metrics =====

    @Bean
    Counter personCreateCounter(MeterRegistry registry) {
        registry.counter("person.operations.total",
                "operation", "create", "result", "success")
    }

    @Bean
    Counter personUpdateCounter(MeterRegistry registry) {
        registry.counter("person.operations.total",
                "operation", "update", "result", "success")
    }

    @Bean
    Counter personDeleteSuccessCounter(MeterRegistry registry) {
        registry.counter("person.operations.total",
                "operation", "delete", "result", "success")
    }

    @Bean
    Counter personDeleteFailureCounter(MeterRegistry registry) {
        registry.counter("person.operations.total",
                "operation", "delete", "result", "failed")
    }

    @Bean
    Counter personReadCounter(MeterRegistry registry) {
        registry.counter("person.operations.total",
                "operation", "read", "result", "success")
    }

    @Bean
    Counter validationErrorCounter(MeterRegistry registry) {
        registry.counter("person.validation.errors.total")
    }

    @Bean
    Timer saveTimer(MeterRegistry registry) {
        registry.timer("person.operation.duration", "operation", "save")
    }

    @Bean
    Timer deleteTimer(MeterRegistry registry) {
        registry.timer("person.operation.duration", "operation", "delete")
    }

    @Bean
    Timer findByIdTimer(MeterRegistry registry) {
        registry.timer("person.operation.duration", "operation", "findById")
    }

    @Bean
    Timer findAllTimer(MeterRegistry registry) {
        registry.timer("person.operation.duration", "operation", "findAll")
    }

    // Repository count gauge - registered directly, not as bean
    @Bean
    String personRepositoryGauge(MeterRegistry registry, PersonRepository personRepository) {
        registry.gauge("person.repository.count", personRepository,
                repo -> repo.count().doubleValue())
        return "personRepositoryGauge"
    }

    // ===== Controller/API Layer Metrics =====

    @Bean
    Counter apiListRequestCounter(MeterRegistry registry) {
        registry.counter("person.api.requests.total",
                "endpoint", "/people", "method", "GET")
    }

    @Bean
    Counter apiGetRequestCounter(MeterRegistry registry) {
        registry.counter("person.api.requests.total",
                "endpoint", "/people/{id}", "method", "GET")
    }

    @Bean
    Counter apiCreateRequestCounter(MeterRegistry registry) {
        registry.counter("person.api.requests.total",
                "endpoint", "/people", "method", "POST")
    }

    @Bean
    Counter apiUpdateRequestCounter(MeterRegistry registry) {
        registry.counter("person.api.requests.total",
                "endpoint", "/people", "method", "PUT")
    }

    @Bean
    Counter apiDeleteRequestCounter(MeterRegistry registry) {
        registry.counter("person.api.requests.total",
                "endpoint", "/people/{id}", "method", "DELETE")
    }

    @Bean
    Timer apiListTimer(MeterRegistry registry) {
        registry.timer("person.api.response.time",
                "endpoint", "/people", "method", "GET")
    }

    @Bean
    Timer apiGetTimer(MeterRegistry registry) {
        registry.timer("person.api.response.time",
                "endpoint", "/people/{id}", "method", "GET")
    }

    @Bean
    Timer apiCreateTimer(MeterRegistry registry) {
        registry.timer("person.api.response.time",
                "endpoint", "/people", "method", "POST")
    }

    @Bean
    Timer apiUpdateTimer(MeterRegistry registry) {
        registry.timer("person.api.response.time",
                "endpoint", "/people", "method", "PUT")
    }

    @Bean
    Timer apiDeleteTimer(MeterRegistry registry) {
        registry.timer("person.api.response.time",
                "endpoint", "/people/{id}", "method", "DELETE")
    }
}
