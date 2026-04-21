package io.github.joxebus.controller

import io.github.joxebus.domain.Person
import io.github.joxebus.service.PersonService
import groovy.util.logging.Slf4j
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping('/people')
@Slf4j
class PersonController {

    @Autowired
    PersonService personService

    @Autowired
    MeterRegistry meterRegistry

    // API request counters
    private Counter apiListRequestCounter
    private Counter apiGetRequestCounter
    private Counter apiCreateRequestCounter
    private Counter apiUpdateRequestCounter
    private Counter apiDeleteRequestCounter

    // API response timers
    private Timer apiListTimer
    private Timer apiGetTimer
    private Timer apiCreateTimer
    private Timer apiUpdateTimer
    private Timer apiDeleteTimer

    @PostConstruct
    void initMetrics() {
        // Request counters by endpoint and method
        apiListRequestCounter = meterRegistry.counter("person.api.requests.total",
                "endpoint", "/people", "method", "GET")
        apiGetRequestCounter = meterRegistry.counter("person.api.requests.total",
                "endpoint", "/people/{id}", "method", "GET")
        apiCreateRequestCounter = meterRegistry.counter("person.api.requests.total",
                "endpoint", "/people", "method", "POST")
        apiUpdateRequestCounter = meterRegistry.counter("person.api.requests.total",
                "endpoint", "/people", "method", "PUT")
        apiDeleteRequestCounter = meterRegistry.counter("person.api.requests.total",
                "endpoint", "/people/{id}", "method", "DELETE")

        // Response time timers
        apiListTimer = meterRegistry.timer("person.api.response.time",
                "endpoint", "/people", "method", "GET")
        apiGetTimer = meterRegistry.timer("person.api.response.time",
                "endpoint", "/people/{id}", "method", "GET")
        apiCreateTimer = meterRegistry.timer("person.api.response.time",
                "endpoint", "/people", "method", "POST")
        apiUpdateTimer = meterRegistry.timer("person.api.response.time",
                "endpoint", "/people", "method", "PUT")
        apiDeleteTimer = meterRegistry.timer("person.api.response.time",
                "endpoint", "/people/{id}", "method", "DELETE")
    }

    @CrossOrigin(origins = "http://localhost:8080")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    List<Person> restList() {
        log.info "Get list of persons as JSON"
        apiListRequestCounter.increment()
        apiListTimer.recordCallable(() -> {
            return personService.findAll()
        })
    }

    @CrossOrigin(origins = "http://localhost:8080")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    Person restSave(@RequestBody Person person) {
        log.info "Saving person $person"
        apiCreateRequestCounter.increment()
        apiCreateTimer.recordCallable(() -> {
            try {
                def savedPerson = personService.save(person)
                meterRegistry.counter("person.api.requests.total",
                        "endpoint", "/people", "method", "POST", "status", "2xx").increment()
                return savedPerson
            } catch (Exception e) {
                meterRegistry.counter("person.api.requests.total",
                        "endpoint", "/people", "method", "POST", "status", "4xx").increment()
                throw e
            }
        })
    }

    @CrossOrigin(origins = "http://localhost:8080")
    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    Person restUpdate(@RequestBody Person person) {
        log.info "Updating person $person"
        apiUpdateRequestCounter.increment()
        apiUpdateTimer.recordCallable(() -> {
            try {
                def updatedPerson = personService.save(person)
                meterRegistry.counter("person.api.requests.total",
                        "endpoint", "/people", "method", "PUT", "status", "2xx").increment()
                return updatedPerson
            } catch (Exception e) {
                meterRegistry.counter("person.api.requests.total",
                        "endpoint", "/people", "method", "PUT", "status", "4xx").increment()
                throw e
            }
        })
    }

    @CrossOrigin(origins = "http://localhost:8080")
    @GetMapping(value = '/{id}',
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    Person restGetPerson(@PathVariable Long id) {
        log.info "Get person with id: $id"
        apiGetRequestCounter.increment()
        apiGetTimer.recordCallable(() -> {
            def person = personService.findById(id)
            if (person) {
                meterRegistry.counter("person.api.requests.total",
                        "endpoint", "/people/{id}", "method", "GET", "status", "2xx").increment()
            } else {
                meterRegistry.counter("person.api.requests.total",
                        "endpoint", "/people/{id}", "method", "GET", "status", "4xx").increment()
            }
            return person
        })
    }

    @CrossOrigin(origins = "http://localhost:8080")
    @DeleteMapping(value = '/{id}',
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<?> restDeletePerson(@PathVariable Long id) {
        log.info "Delete person with id: $id"
        apiDeleteRequestCounter.increment()

        def result = apiDeleteTimer.recordCallable(() -> {
            HttpStatus status = null
            try {
                if (personService.delete(id)) {
                    log.debug("SUCCESS")
                    status = HttpStatus.OK
                    meterRegistry.counter("person.api.requests.total",
                            "endpoint", "/people/{id}", "method", "DELETE", "status", "2xx").increment()
                } else {
                    log.error("ERROR")
                    status = HttpStatus.UNPROCESSABLE_ENTITY
                    meterRegistry.counter("person.api.requests.total",
                            "endpoint", "/people/{id}", "method", "DELETE", "status", "4xx").increment()
                }
            } catch (Exception e) {
                log.error(e.getMessage())
                status = HttpStatus.BAD_REQUEST
                meterRegistry.counter("person.api.requests.total",
                        "endpoint", "/people/{id}", "method", "DELETE", "status", "4xx").increment()
            }
            return status
        })

        return new ResponseEntity<String>(result)
    }
}
