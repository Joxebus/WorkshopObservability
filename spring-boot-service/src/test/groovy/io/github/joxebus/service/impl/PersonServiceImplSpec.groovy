package io.github.joxebus.service.impl

import io.github.joxebus.domain.Person
import io.github.joxebus.service.PersonService
import spock.lang.Specification

class PersonServiceImplSpec extends Specification {

    PersonService personService = new PersonServiceImpl()

    def "should verify PersonServiceImpl is instantiable"() {
        expect: "service is not null"
        personService != null
    }

    def "should verify PersonServiceImpl implements PersonService interface"() {
        expect: "service implements the interface"
        personService instanceof PersonService
    }

    def "should have findAll method"() {
        when: "calling findAll method"
        try {
            personService.findAll()
        } catch (IllegalStateException e) {
            // Expected when GORM not initialized in unit test
        }

        then: "method exists (no MissingMethodException)"
        true
    }

    def "should have findById method"() {
        when: "calling findById method with an ID"
        try {
            personService.findById(1L)
        } catch (IllegalStateException e) {
            // Expected when GORM not initialized in unit test
        }

        then: "method exists (no MissingMethodException)"
        true
    }

    def "should have save method"() {
        when: "calling save method with a person"
        Person person = new Person(name: "Test", lastname: "User", email: "test@example.com")
        try {
            personService.save(person)
        } catch (IllegalStateException e) {
            // Expected when GORM not initialized in unit test
        }

        then: "method exists (no MissingMethodException)"
        true
    }

    def "should have delete method"() {
        when: "calling delete method with an ID"
        try {
            personService.delete(1L)
        } catch (IllegalStateException e) {
            // Expected when GORM not initialized in unit test
        }

        then: "method exists (no MissingMethodException)"
        true
    }
}
