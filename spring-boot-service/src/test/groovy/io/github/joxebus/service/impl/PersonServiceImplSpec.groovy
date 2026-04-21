package io.github.joxebus.service.impl

import io.github.joxebus.domain.Person
import io.github.joxebus.repository.PersonRepository
import io.github.joxebus.service.PersonService
import jakarta.validation.ConstraintViolation
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validator
import spock.lang.Specification
import spock.lang.Subject

class PersonServiceImplSpec extends Specification {

    PersonRepository personRepository = Mock()
    Validator validator = Mock()

    @Subject
    PersonService personService = new PersonServiceImpl(
        personRepository: personRepository,
        validator: validator
    )

    def "should find all persons"() {
        given: "a list of persons in the repository"
        List<Person> persons = [
            new Person(id: 1L, name: "Omar", lastname: "Bautista", email: "omar@email.com"),
            new Person(id: 2L, name: "Jorge", lastname: "Valenzuela", email: "jorge@email.com")
        ]
        personRepository.findAll() >> persons

        when: "calling findAll"
        List<Person> result = personService.findAll()

        then: "all persons are returned"
        result.size() == 2
        result[0].name == "Omar"
        result[1].name == "Jorge"
    }

    def "should find person by id"() {
        given: "a person exists in the repository"
        Person person = new Person(id: 1L, name: "Omar", lastname: "Bautista", email: "omar@email.com")
        personRepository.findById(1L) >> Optional.of(person)

        when: "calling findById with existing id"
        Person result = personService.findById(1L)

        then: "the person is returned"
        result != null
        result.id == 1L
        result.name == "Omar"
        result.email == "omar@email.com"
    }

    def "should return null when person not found"() {
        given: "person does not exist in repository"
        personRepository.findById(999L) >> Optional.empty()

        when: "calling findById with non-existing id"
        Person result = personService.findById(999L)

        then: "null is returned"
        result == null
    }

    def "should save valid person"() {
        given: "a valid person"
        Person person = new Person(name: "Test", lastname: "User", email: "test@example.com")
        Person savedPerson = new Person(id: 1L, name: "Test", lastname: "User", email: "test@example.com")

        when: "calling save"
        Person result = personService.save(person)

        then: "person is saved with generated id"
        1 * validator.validate(person) >> new HashSet<ConstraintViolation<Person>>()
        1 * personRepository.save(person) >> savedPerson
        result.id == 1L
        result.name == "Test"
        result.email == "test@example.com"
    }

    def "should throw exception when saving invalid person"() {
        given: "an invalid person with validation errors"
        Person person = new Person(name: "", lastname: "User", email: "invalid-email")
        ConstraintViolation<Person> violation = Mock()

        when: "calling save with invalid person"
        personService.save(person)

        then: "ConstraintViolationException is thrown"
        1 * validator.validate(person) >> ([violation] as Set)
        0 * personRepository.save(_)
        thrown(ConstraintViolationException)
    }

    def "should delete existing person"() {
        given: "a person exists in the repository"
        Person person = new Person(id: 1L, name: "Omar", lastname: "Bautista", email: "omar@email.com")

        when: "calling delete with existing id"
        boolean result = personService.delete(1L)

        then: "person is deleted and true is returned"
        1 * personRepository.findById(1L) >> Optional.of(person)
        1 * personRepository.delete(person)
        result == true
    }

    def "should return false when deleting non-existing person"() {
        when: "calling delete with non-existing id"
        boolean result = personService.delete(999L)

        then: "false is returned and delete is not called"
        1 * personRepository.findById(999L) >> Optional.empty()
        0 * personRepository.deleteById(_)
        result == false
    }

    def "should validate person fields on save"() {
        given: "a person with blank fields"
        Person person = new Person(name: "", lastname: "", email: "")
        ConstraintViolation<Person> violation1 = Mock()
        ConstraintViolation<Person> violation2 = Mock()
        ConstraintViolation<Person> violation3 = Mock()

        when: "calling save"
        personService.save(person)

        then: "validation exception is thrown with all violations"
        1 * validator.validate(person) >> ([violation1, violation2, violation3] as Set)
        ConstraintViolationException ex = thrown()
        ex.constraintViolations.size() == 3
    }

    def "should handle null person in save"() {
        when: "calling save with null"
        personService.save(null)

        then: "NullPointerException is thrown"
        thrown(NullPointerException)
    }

    def "should find all returns empty list when no persons"() {
        given: "repository is empty"
        personRepository.findAll() >> []

        when: "calling findAll"
        List<Person> result = personService.findAll()

        then: "empty list is returned"
        result != null
        result.isEmpty()
    }
}
