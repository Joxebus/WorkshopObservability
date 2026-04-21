package io.github.joxebus.config

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Centralized configuration for frontend application metrics.
 * All Counter and Timer beans are initialized here for cleaner dependency injection.
 */
@Configuration
class MetricsConfig {

    // ===== Frontend Operation Counters =====

    @Bean
    Counter frontendListSuccessCounter(MeterRegistry registry) {
        registry.counter("person.frontend.requests.total",
                "operation", "list", "result", "success")
    }

    @Bean
    Counter frontendListErrorCounter(MeterRegistry registry) {
        registry.counter("person.frontend.requests.total",
                "operation", "list", "result", "error")
    }

    @Bean
    Counter frontendCreateSuccessCounter(MeterRegistry registry) {
        registry.counter("person.frontend.requests.total",
                "operation", "create", "result", "success")
    }

    @Bean
    Counter frontendCreateErrorCounter(MeterRegistry registry) {
        registry.counter("person.frontend.requests.total",
                "operation", "create", "result", "error")
    }

    @Bean
    Counter frontendDeleteSuccessCounter(MeterRegistry registry) {
        registry.counter("person.frontend.requests.total",
                "operation", "delete", "result", "success")
    }

    @Bean
    Counter frontendDeleteErrorCounter(MeterRegistry registry) {
        registry.counter("person.frontend.requests.total",
                "operation", "delete", "result", "error")
    }

    // ===== Backend Communication Error Counters =====

    @Bean
    Counter backendListErrorCounter(MeterRegistry registry) {
        registry.counter("person.frontend.backend.errors.total",
                "operation", "list")
    }

    @Bean
    Counter backendCreateErrorCounter(MeterRegistry registry) {
        registry.counter("person.frontend.backend.errors.total",
                "operation", "create")
    }

    @Bean
    Counter backendDeleteErrorCounter(MeterRegistry registry) {
        registry.counter("person.frontend.backend.errors.total",
                "operation", "delete")
    }

    // ===== Page Render Timers =====

    @Bean
    Timer listPageRenderTimer(MeterRegistry registry) {
        registry.timer("person.frontend.page.render.time",
                "page", "list")
    }

    @Bean
    Timer createPageRenderTimer(MeterRegistry registry) {
        registry.timer("person.frontend.page.render.time",
                "page", "create")
    }
}
