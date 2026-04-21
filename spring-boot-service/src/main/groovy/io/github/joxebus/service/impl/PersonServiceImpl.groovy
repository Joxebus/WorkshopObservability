package io.github.joxebus.service.impl

import io.github.joxebus.domain.Person
import io.github.joxebus.repository.PersonRepository
import io.github.joxebus.service.PersonService
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import jakarta.annotation.PostConstruct
import jakarta.validation.ConstraintViolation
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

    // Counters for business operations
    private Counter personCreateCounter
    private Counter personUpdateCounter
    private Counter personDeleteSuccessCounter
    private Counter personDeleteFailureCounter
    private Counter personReadCounter
    private Counter validationErrorCounter

    // Timers for operation duration
    private Timer saveTimer
    private Timer deleteTimer
    private Timer findByIdTimer
    private Timer findAllTimer

    @PostConstruct
    void initMetrics() {
        // Operation counters with result tags
        personCreateCounter = meterRegistry.counter("person.operations.total",
                "operation", "create", "result", "success")
        personUpdateCounter = meterRegistry.counter("person.operations.total",
                "operation", "update", "result", "success")
        personDeleteSuccessCounter = meterRegistry.counter("person.operations.total",
                "operation", "delete", "result", "success")
        personDeleteFailureCounter = meterRegistry.counter("person.operations.total",
                "operation", "delete", "result", "failed")
        personReadCounter = meterRegistry.counter("person.operations.total",
                "operation", "read", "result", "success")

        // Validation error counter
        validationErrorCounter = meterRegistry.counter("person.validation.errors.total")

        // Operation timers
        saveTimer = meterRegistry.timer("person.operation.duration", "operation", "save")
        deleteTimer = meterRegistry.timer("person.operation.duration", "operation", "delete")
        findByIdTimer = meterRegistry.timer("person.operation.duration", "operation", "findById")
        findAllTimer = meterRegistry.timer("person.operation.duration", "operation", "findAll")

        // Gauge for current person count
        meterRegistry.gauge("person.repository.count", personRepository, repo -> repo.count().doubleValue())
    }

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
