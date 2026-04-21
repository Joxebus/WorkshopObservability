package io.github.joxebus.domain

import jakarta.validation.ConstraintViolation
import jakarta.validation.Validation
import jakarta.validation.Validator
import jakarta.validation.ValidatorFactory
import spock.lang.Specification

class PersonSpec extends Specification {

    Validator validator

    def setup() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory()
        validator = factory.getValidator()
    }

    def "should create valid person"() {
        given: "a person with valid data"
        Person person = new Person(
            name: "Omar",
            lastname: "Bautista",
            email: "omar@email.com"
        )

        when: "validating the person"
        Set<ConstraintViolation<Person>> violations = validator.validate(person)

        then: "no violations"
        violations.isEmpty()
    }

    def "should reject person with blank name"() {
        given: "a person with blank name"
        Person person = new Person(
            name: "",
            lastname: "Bautista",
            email: "omar@email.com"
        )

        when: "validating the person"
        Set<ConstraintViolation<Person>> violations = validator.validate(person)

        then: "name violation is present"
        !violations.isEmpty()
        violations.any { it.propertyPath.toString() == "name" }
        violations.find { it.propertyPath.toString() == "name" }.message.contains("required")
    }

    def "should reject person with blank lastname"() {
        given: "a person with blank lastname"
        Person person = new Person(
            name: "Omar",
            lastname: "",
            email: "omar@email.com"
        )

        when: "validating the person"
        Set<ConstraintViolation<Person>> violations = validator.validate(person)

        then: "lastname violation is present"
        !violations.isEmpty()
        violations.any { it.propertyPath.toString() == "lastname" }
    }

    def "should reject person with invalid email"() {
        given: "a person with invalid email"
        Person person = new Person(
            name: "Omar",
            lastname: "Bautista",
            email: "not-an-email"
        )

        when: "validating the person"
        Set<ConstraintViolation<Person>> violations = validator.validate(person)

        then: "email violation is present"
        !violations.isEmpty()
        violations.any { it.propertyPath.toString() == "email" }
        violations.find { it.propertyPath.toString() == "email" }.message.contains("valid")
    }

    def "should reject person with name too long"() {
        given: "a person with name exceeding 30 characters"
        Person person = new Person(
            name: "a" * 31,
            lastname: "Bautista",
            email: "omar@email.com"
        )

        when: "validating the person"
        Set<ConstraintViolation<Person>> violations = validator.validate(person)

        then: "size violation is present"
        !violations.isEmpty()
        violations.any { it.propertyPath.toString() == "name" }
    }

    def "should reject person with lastname too long"() {
        given: "a person with lastname exceeding 30 characters"
        Person person = new Person(
            name: "Omar",
            lastname: "a" * 31,
            email: "omar@email.com"
        )

        when: "validating the person"
        Set<ConstraintViolation<Person>> violations = validator.validate(person)

        then: "size violation is present"
        !violations.isEmpty()
        violations.any { it.propertyPath.toString() == "lastname" }
    }

    def "should accept person with maximum allowed length"() {
        given: "a person with fields at maximum allowed length"
        Person person = new Person(
            name: "a" * 30,
            lastname: "b" * 30,
            email: "test@example.com"
        )

        when: "validating the person"
        Set<ConstraintViolation<Person>> violations = validator.validate(person)

        then: "no violations"
        violations.isEmpty()
    }

    def "should have all violations for completely invalid person"() {
        given: "a person with all invalid fields"
        Person person = new Person(
            name: "",
            lastname: "",
            email: "invalid"
        )

        when: "validating the person"
        Set<ConstraintViolation<Person>> violations = validator.validate(person)

        then: "multiple violations are present"
        violations.size() >= 3
    }

    def "should accept various valid email formats"() {
        given: "persons with different valid email formats"
        List<String> validEmails = [
            "simple@example.com",
            "test.user@example.com",
            "user+tag@example.co.uk",
            "user_name@example-domain.com"
        ]

        expect: "all emails are valid"
        validEmails.every { email ->
            Person person = new Person(name: "Test", lastname: "User", email: email)
            validator.validate(person).isEmpty()
        }
    }

    def "should reject various invalid email formats"() {
        given: "persons with different invalid email formats"
        List<String> invalidEmails = [
            "plaintext",
            "@example.com",
            "user@",
            "user name@example.com",
            "user@example"
        ]

        expect: "all emails are invalid"
        invalidEmails.every { email ->
            Person person = new Person(name: "Test", lastname: "User", email: email)
            !validator.validate(person).isEmpty()
        }
    }

    def "should have proper toString representation"() {
        given: "a person"
        Person person = new Person(
            id: 1L,
            name: "Omar",
            lastname: "Bautista",
            email: "omar@email.com"
        )

        when: "calling toString"
        String result = person.toString()

        then: "string contains person data"
        result.contains("Omar")
        result.contains("Bautista")
        result.contains("omar@email.com")
    }

    def "should support equals and hashCode"() {
        given: "two persons with same data"
        Person person1 = new Person(id: 1L, name: "Omar", lastname: "Bautista", email: "omar@email.com")
        Person person2 = new Person(id: 1L, name: "Omar", lastname: "Bautista", email: "omar@email.com")

        expect: "they are equal"
        person1 == person2
        person1.hashCode() == person2.hashCode()
    }

    def "should distinguish different persons"() {
        given: "two persons with different ids"
        Person person1 = new Person(id: 1L, name: "Omar", lastname: "Bautista", email: "omar@email.com")
        Person person2 = new Person(id: 2L, name: "Jorge", lastname: "Valenzuela", email: "jorge@email.com")

        expect: "they are not equal"
        person1 != person2
    }
}
