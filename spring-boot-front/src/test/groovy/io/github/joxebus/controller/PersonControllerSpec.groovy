package io.github.joxebus.controller

import io.github.joxebus.domain.Person
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.http.HttpEntity
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.ui.Model
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import spock.lang.Specification
import spock.lang.Subject

class PersonControllerSpec extends Specification {

    RestTemplate restTemplate = Mock()
    Model model = Mock()
    RedirectAttributes redirectAttributes = Mock()
    Counter mockCounter = Mock()
    Timer mockTimer = Mock()

    @Subject
    PersonController personController

    def setup() {
        personController = new PersonController(
            restTemplate: restTemplate
        )

        // Initialize metrics (injected as beans in production via MetricsConfig)
        personController.frontendListSuccessCounter = mockCounter
        personController.frontendListErrorCounter = mockCounter
        personController.frontendCreateSuccessCounter = mockCounter
        personController.frontendCreateErrorCounter = mockCounter
        personController.frontendDeleteSuccessCounter = mockCounter
        personController.frontendDeleteErrorCounter = mockCounter
        personController.backendListErrorCounter = mockCounter
        personController.backendCreateErrorCounter = mockCounter
        personController.backendDeleteErrorCounter = mockCounter
        personController.listPageRenderTimer = mockTimer
        personController.createPageRenderTimer = mockTimer

        // Mock Timer.recordCallable to execute the closure and return result
        mockTimer.recordCallable(_) >> { args ->
            def callable = args[0]
            return callable.call()
        }
    }

    // List Tests
    def "should display list of people successfully"() {
        given: "backend returns list of persons"
        List<Person> persons = [
            new Person(id: 1L, name: "John", lastname: "Doe", email: "john@example.com"),
            new Person(id: 2L, name: "Jane", lastname: "Smith", email: "jane@example.com")
        ]

        when: "calling list method"
        def result = personController.list(model)

        then: "persons are loaded and view is returned"
        1 * restTemplate.getForObject("/people", List) >> persons
        1 * model.addAttribute('listOfPeople', persons)
        1 * mockCounter.increment()  // frontendListSuccessCounter
        result == 'person/list'
    }

    def "should handle error when loading list"() {
        when: "calling list method and backend fails"
        def result = personController.list(model)

        then: "error is added to model"
        1 * restTemplate.getForObject("/people", List) >> { throw new RestClientException("Backend error") }
        1 * model.addAttribute("error", { it.contains("Can't load data") })
        1 * mockCounter.increment()  // frontendListErrorCounter
        1 * mockCounter.increment()  // backendListErrorCounter
        result == 'person/list'
    }

    // New Person Tests
    def "should display new person form"() {
        when: "calling newPerson method"
        def result = personController.newPerson(model)

        then: "empty person is added to model"
        1 * model.addAttribute("person", _ as Person)
        result == 'person/create'
    }

    // Create Person Tests
    def "should create person successfully"() {
        given: "a valid person"
        Person person = new Person(name: "Test", lastname: "User", email: "test@example.com")
        Person savedPerson = new Person(id: 1L, name: "Test", lastname: "User", email: "test@example.com")
        ResponseEntity<Person> response = new ResponseEntity<>(savedPerson, HttpStatus.OK)

        when: "calling createPerson method"
        def result = personController.createPerson(person, model, redirectAttributes)

        then: "person is created and redirected"
        1 * restTemplate.postForEntity("/people", _ as HttpEntity, Person) >> response
        1 * mockCounter.increment()  // frontendCreateSuccessCounter
        1 * redirectAttributes.addFlashAttribute("message", { it.contains("Successfuly added Test") })
        result == 'redirect:/people'
    }

    def "should handle validation error when creating person"() {
        given: "an invalid person"
        Person person = new Person(name: "", lastname: "User", email: "invalid")
        ResponseEntity<Person> response = new ResponseEntity<>(person, HttpStatus.BAD_REQUEST)

        when: "calling createPerson method"
        def result = personController.createPerson(person, model, redirectAttributes)

        then: "validation error is shown"
        1 * restTemplate.postForEntity("/people", _ as HttpEntity, Person) >> response
        1 * mockCounter.increment()  // frontendCreateErrorCounter
        1 * model.addAttribute("person", person)
        1 * model.addAttribute("error", "Verify your information")
        result == 'person/create'
    }

    def "should handle exception when creating person"() {
        given: "a person that causes backend error"
        Person person = new Person(name: "Test", lastname: "User", email: "test@example.com")

        when: "calling createPerson and backend throws exception"
        def result = personController.createPerson(person, model, redirectAttributes)

        then: "error is displayed"
        1 * restTemplate.postForEntity("/people", _ as HttpEntity, Person) >> { throw new RestClientException("Backend error") }
        1 * mockCounter.increment()  // frontendCreateErrorCounter
        1 * mockCounter.increment()  // backendCreateErrorCounter
        1 * model.addAttribute("person", person)
        1 * model.addAttribute("error", "Verify your information")
        result == 'person/create'
    }

    // Delete Person Tests
    def "should delete person successfully"() {
        when: "calling delete method"
        def result = personController.delete(1L, redirectAttributes)

        then: "person is deleted and redirected"
        1 * restTemplate.delete("/people/1")
        1 * mockCounter.increment()  // frontendDeleteSuccessCounter
        1 * redirectAttributes.addFlashAttribute("message", { it.contains("Successfuly deleted") })
        result == 'redirect:/people'
    }

    def "should handle error when deleting person"() {
        when: "calling delete and backend fails"
        def result = personController.delete(1L, redirectAttributes)

        then: "error message is shown"
        1 * restTemplate.delete("/people/1") >> { throw new RestClientException("Backend error") }
        1 * mockCounter.increment()  // frontendDeleteErrorCounter
        1 * mockCounter.increment()  // backendDeleteErrorCounter
        1 * redirectAttributes.addFlashAttribute("error", "Verify your information")
        result == 'redirect:/people'
    }

    // Edge Cases
    def "should handle empty list from backend"() {
        given: "backend returns empty list"
        List<Person> emptyList = []

        when: "calling list method"
        def result = personController.list(model)

        then: "empty list is displayed"
        1 * restTemplate.getForObject("/people", List) >> emptyList
        1 * model.addAttribute('listOfPeople', emptyList)
        1 * mockCounter.increment()  // frontendListSuccessCounter
        result == 'person/list'
    }

    def "should handle null response from backend on list"() {
        when: "calling list and backend returns null"
        def result = personController.list(model)

        then: "error is handled"
        1 * restTemplate.getForObject("/people", List) >> null
        1 * model.addAttribute('listOfPeople', null)
        1 * mockCounter.increment()  // frontendListSuccessCounter
        result == 'person/list'
    }
}
