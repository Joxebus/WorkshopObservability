package io.github.joxebus.config

import io.github.joxebus.domain.Person
import io.github.joxebus.repository.PersonRepository
import io.github.joxebus.service.PersonService
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationContext
import org.springframework.context.event.EventListener
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

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
    PersonRepository personRepository

    @Autowired
    ApplicationContext ctx

    @EventListener
    @Transactional
    def init(ApplicationReadyEvent ready) {

        if (personRepository.count() == 0) {
            log.info "--------- INIT Loading information ---------"
            def persons = [
                    new Person(name: "Omar", lastname: "Bautista", email: 'obautista@email.com'),
                    new Person(name: "Jorge", lastname: "Valenzuela", email: 'jvalenzuela@email.com')
            ]

            log.info("About to load users: ${persons}")

            // Save person if validation constraints are met
            persons.each { person ->
                try {
                    personService.save(person)
                    log.info("Successfully saved ${person}")
                } catch (Exception e) {
                    log.error("Failed to save ${person}: ${e.message}")
                }
            }
            log.info "--------- FINISH Loading information ---------"
        } else {
            log.info("--------- Nothing to load ---------")
        }
    }
}
