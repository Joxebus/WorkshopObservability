package io.github.joxebus.service.impl

import io.github.joxebus.domain.Person
import io.github.joxebus.repository.PersonRepository
import io.github.joxebus.service.PersonService
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import jakarta.validation.ConstraintViolation
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
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

    // Counters for business operations - injected from MetricsConfig
    @Autowired
    @Qualifier("personCreateCounter")
    Counter personCreateCounter

    @Autowired
    @Qualifier("personUpdateCounter")
    Counter personUpdateCounter

    @Autowired
    @Qualifier("personDeleteSuccessCounter")
    Counter personDeleteSuccessCounter

    @Autowired
    @Qualifier("personDeleteFailureCounter")
    Counter personDeleteFailureCounter

    @Autowired
    @Qualifier("personReadCounter")
    Counter personReadCounter

    @Autowired
    @Qualifier("validationErrorCounter")
    Counter validationErrorCounter

    // Timers for operation duration - injected from MetricsConfig
    @Autowired
    @Qualifier("saveTimer")
    Timer saveTimer

    @Autowired
    @Qualifier("deleteTimer")
    Timer deleteTimer

    @Autowired
    @Qualifier("findByIdTimer")
    Timer findByIdTimer

    @Autowired
    @Qualifier("findAllTimer")
    Timer findAllTimer

    @Override
    List<Person> findAll() {
        findAllTimer.recordCallable(() -> {
            personReadCounter.increment()
            return personRepository.findAll()
        })
    }

    @Override
    Person findById(Long id) {
        findByIdTimer.recordCallable(() -> {
            def person = personRepository.findById(id).orElse(null)
            if (person) {
                personReadCounter.increment()
            }
            return person
        })
    }

    @Override
    Person save(Person person) {
        saveTimer.recordCallable(() -> {
            def violations = validator.validate(person)
            if (!violations.empty) {
                validationErrorCounter.increment()
                // Track validation errors by field
                violations.each { ConstraintViolation violation ->
                    meterRegistry.counter("person.validation.errors.total",
                            "field", violation.propertyPath.toString()).increment()
                }
                throw new ConstraintViolationException("Person fields are incorrect", violations)
            }

            def savedPerson = personRepository.save(person)

            // Track create vs update
            if (person.id == null) {
                personCreateCounter.increment()
            } else {
                personUpdateCounter.increment()
            }

            return savedPerson
        })
    }

    @Override
    boolean delete(Long id) {
        deleteTimer.recordCallable(() -> {
            Person person = findById(id)
            if (person) {
                personRepository.delete(person)
                personDeleteSuccessCounter.increment()
                return true
            } else {
                personDeleteFailureCounter.increment()
                meterRegistry.counter("person.delete.failures.total").increment()
                return false
            }
        })
    }

    @Override
    long count() {
        personRepository.count()
    }
}
