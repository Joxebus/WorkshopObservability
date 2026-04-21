package io.github.joxebus.controller

import io.github.joxebus.domain.Person
import io.github.joxebus.service.PersonService
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import spock.lang.Specification
import spock.lang.Subject

class PersonControllerSpec extends Specification {

    PersonService personService = Mock()
    MeterRegistry meterRegistry = Mock()
    Counter mockCounter = Mock()
    Timer mockTimer = Mock()

    @Subject
    PersonController personController = new PersonController(
        personService: personService,
        meterRegistry: meterRegistry,
        apiListRequestCounter: mockCounter,
        apiGetRequestCounter: mockCounter,
        apiCreateRequestCounter: mockCounter,
        apiUpdateRequestCounter: mockCounter,
        apiDeleteRequestCounter: mockCounter,
        apiListTimer: mockTimer,
        apiGetTimer: mockTimer,
        apiCreateTimer: mockTimer,
        apiUpdateTimer: mockTimer,
        apiDeleteTimer: mockTimer
    )

    def setup() {
        // Mock Timer.recordCallable to execute the closure and return result
        mockTimer.recordCallable(_) >> { args ->
            def callable = args[0]
            return callable.call()
        }
        // Mock meterRegistry.counter() to return mockCounter for dynamic counters
        // Use wildcard matcher to handle varargs with any number of parameters
        meterRegistry.counter(*_) >> mockCounter
    }

    def "should get list of all persons"() {
        given: "service returns list of persons"
        List<Person> persons = [
            new Person(id: 1L, name: "Omar", lastname: "Bautista", email: "omar@email.com"),
            new Person(id: 2L, name: "Jorge", lastname: "Valenzuela", email: "jorge@email.com")
        ]

        when: "calling restList"
        List<Person> result = personController.restList()

        then: "list of persons is returned"
        1 * personService.findAll() >> persons
        result.size() == 2
        result[0].name == "Omar"
        result[1].name == "Jorge"
    }

    def "should get empty list when no persons"() {
        given: "service returns empty list"
        personService.findAll() >> []

        when: "calling restList"
        List<Person> result = personController.restList()

        then: "empty list is returned"
        result.isEmpty()
    }

    def "should create new person"() {
        given: "a valid person to save"
        Person person = new Person(name: "Test", lastname: "User", email: "test@example.com")
        Person savedPerson = new Person(id: 1L, name: "Test", lastname: "User", email: "test@example.com")

        when: "calling restSave"
        Person result = personController.restSave(person)

        then: "saved person with id is returned"
        1 * personService.save(person) >> savedPerson
        result.id == 1L
        result.name == "Test"
        result.email == "test@example.com"
    }

    def "should update existing person"() {
        given: "an existing person to update"
        Person person = new Person(id: 1L, name: "Updated", lastname: "User", email: "updated@example.com")

        when: "calling restUpdate"
        Person result = personController.restUpdate(person)

        then: "updated person is returned"
        1 * personService.save(person) >> person
        result.id == 1L
        result.name == "Updated"
        result.email == "updated@example.com"
    }

    def "should get person by id"() {
        given: "a person exists with given id"
        Person person = new Person(id: 1L, name: "Omar", lastname: "Bautista", email: "omar@email.com")

        when: "calling restGetPerson"
        Person result = personController.restGetPerson(1L)

        then: "person is returned"
        1 * personService.findById(1L) >> person
        result.id == 1L
        result.name == "Omar"
    }

    def "should return null when person not found by id"() {
        given: "person does not exist"
        personService.findById(999L) >> null

        when: "calling restGetPerson"
        Person result = personController.restGetPerson(999L)

        then: "null is returned"
        result == null
    }

    def "should delete person successfully"() {
        when: "calling restDeletePerson"
        ResponseEntity<?> result = personController.restDeletePerson(1L)

        then: "OK status is returned"
        1 * personService.delete(1L) >> true
        result.statusCode == HttpStatus.OK
    }

    def "should return unprocessable entity when delete fails"() {
        given: "person deletion fails"
        personService.delete(999L) >> false

        when: "calling restDeletePerson"
        ResponseEntity<?> result = personController.restDeletePerson(999L)

        then: "UNPROCESSABLE_ENTITY status is returned"
        result.statusCode == HttpStatus.UNPROCESSABLE_ENTITY
        1 * personService.delete(999L)
    }

    def "should return bad request on delete error"() {
        given: "service throws exception on delete"
        personService.delete(1L) >> { throw new RuntimeException("Test error") }

        when: "calling restDeletePerson"
        ResponseEntity<?> result = personController.restDeletePerson(1L)

        then: "BAD_REQUEST status is returned"
        result.statusCode == HttpStatus.BAD_REQUEST
    }

    def "should handle multiple persons in list"() {
        given: "service returns multiple persons"
        List<Person> persons = (1..10).collect {
            new Person(id: it, name: "Person$it", lastname: "Last$it", email: "person$it@email.com")
        }
        personService.findAll() >> persons

        when: "calling restList"
        List<Person> result = personController.restList()

        then: "all persons are returned"
        result.size() == 10
        result.every { it.id != null }
        result.every { it.name.startsWith("Person") }
    }

    def "should preserve person data when saving"() {
        given: "a person with specific data"
        Person person = new Person(
            name: "SpecialName",
            lastname: "SpecialLastname",
            email: "special@email.com"
        )
        personService.save(person) >> { Person p ->
            p.id = 123L
            return p
        }

        when: "calling restSave"
        Person result = personController.restSave(person)

        then: "all data is preserved"
        result.id == 123L
        result.name == "SpecialName"
        result.lastname == "SpecialLastname"
        result.email == "special@email.com"
    }
}
