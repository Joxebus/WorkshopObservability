package io.github.joxebus.config

import io.github.joxebus.domain.Person
import io.github.joxebus.service.PersonService
import groovy.util.logging.Slf4j
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationContext
import org.springframework.context.event.EventListener
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

/**
 * This class has been created to load information
 * just after application has been loaded.
 */

@Component
@Profile("!test")
@Slf4j
class Bootstrap {

    @Autowired
    PersonService personService

    @Autowired
    ApplicationContext ctx

    @Autowired
    MeterRegistry meterRegistry

    @EventListener
    def init(ApplicationReadyEvent ready) {
        long startTime = System.currentTimeMillis()

        if (personService.count() == 0) {
            log.info "--------- INIT Loading information ---------"
            def persons = [
                    new Person(name: "Omar", lastname: "Bautista", email: 'obautista@email.com'),
                    new Person(name: "Jorge", lastname: "Valenzuela", email: 'jvalenzuela@email.com')
            ]

            log.info("About to load users: ${persons}")

            // Track bootstrap data loading
            int successCount = 0
            int failureCount = 0

            // Save person if validation constraints are met
            persons.each { person ->
                try {
                    personService.save(person)
                    successCount++
                    meterRegistry.counter("bootstrap.data.loaded.total",
                            "result", "success").increment()
                    log.info("Successfully saved ${person}")
                } catch (Exception e) {
                    failureCount++
                    meterRegistry.counter("bootstrap.data.loaded.total",
                            "result", "failed").increment()
                    log.error("Failed to save ${person}: ${e.message}")
                }
            }

            long duration = System.currentTimeMillis() - startTime
            log.info "--------- FINISH Loading information (${successCount} success, ${failureCount} failed, ${duration}ms) ---------"

            // Record bootstrap execution time
            meterRegistry.timer("bootstrap.execution.time").record(duration, java.util.concurrent.TimeUnit.MILLISECONDS)

            // Create gauge for bootstrap completion status
            meterRegistry.gauge("bootstrap.data.loaded.count", successCount)
        } else {
            log.info("--------- Nothing to load ---------")
            meterRegistry.counter("bootstrap.data.loaded.total",
                    "result", "skipped").increment()
        }
    }
}
