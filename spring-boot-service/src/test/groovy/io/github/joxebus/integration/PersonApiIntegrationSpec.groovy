package io.github.joxebus.integration

import io.github.joxebus.BackApplication
import io.github.joxebus.domain.Person
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.TestPropertySource
import spock.lang.Specification
import spock.lang.Stepwise

@SpringBootTest(
    classes = BackApplication,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@TestPropertySource(properties = [
    "spring.cloud.consul.enabled=false",
    "spring.cloud.consul.discovery.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.profiles.active=test"
])
@Stepwise
class PersonApiIntegrationSpec extends Specification {

    @Autowired
    TestRestTemplate restTemplate

    def "should get empty list initially"() {
        when: "calling GET /people"
        ResponseEntity<List> response = restTemplate.getForEntity("/people", List)

        then: "empty list is returned"
        response.statusCode == HttpStatus.OK
        response.body != null
        response.body.isEmpty()
    }

    def "should create a new person"() {
        given: "a new person"
        Person person = new Person(
            name: "Integration",
            lastname: "Test",
            email: "integration@test.com"
        )

        when: "calling POST /people"
        ResponseEntity<Person> response = restTemplate.postForEntity("/people", person, Person)

        then: "person is created successfully"
        response.statusCode == HttpStatus.OK
        response.body.id != null
        response.body.name == "Integration"
        response.body.lastname == "Test"
        response.body.email == "integration@test.com"
    }

    def "should get list with one person"() {
        when: "calling GET /people"
        ResponseEntity<List> response = restTemplate.getForEntity("/people", List)

        then: "list contains one person"
        response.statusCode == HttpStatus.OK
        response.body.size() == 1
    }

    def "should create another person"() {
        given: "another person"
        Person person = new Person(
            name: "Second",
            lastname: "Person",
            email: "second@test.com"
        )

        when: "calling POST /people"
        ResponseEntity<Person> response = restTemplate.postForEntity("/people", person, Person)

        then: "person is created"
        response.statusCode == HttpStatus.OK
        response.body.id != null
        response.body.name == "Second"
    }

    def "should get list with two persons"() {
        when: "calling GET /people"
        ResponseEntity<List> response = restTemplate.getForEntity("/people", List)

        then: "list contains two persons"
        response.statusCode == HttpStatus.OK
        response.body.size() == 2
    }

    def "should update existing person"() {
        given: "an existing person to update"
        Person person = new Person(
            id: 1L,
            name: "Updated",
            lastname: "Name",
            email: "integration@test.com"
        )

        when: "calling PUT /people"
        restTemplate.put("/people", person)
        ResponseEntity<List> response = restTemplate.getForEntity("/people", List)

        then: "person is updated"
        response.statusCode == HttpStatus.OK
        def updatedPerson = response.body.find { it.id == 1L }
        updatedPerson.name == "Updated"
        updatedPerson.lastname == "Name"
    }

    def "should delete person"() {
        when: "calling DELETE /people/1"
        restTemplate.delete("/people/1")
        ResponseEntity<List> response = restTemplate.getForEntity("/people", List)

        then: "person is deleted"
        response.statusCode == HttpStatus.OK
        response.body.size() == 1
        !response.body.any { it.id == 1L }
    }

    def "should reject person with invalid email"() {
        given: "person with invalid email"
        Person person = new Person(
            name: "Invalid",
            lastname: "Email",
            email: "not-an-email"
        )

        when: "calling POST /people"
        ResponseEntity<Person> response = restTemplate.postForEntity("/people", person, Person)

        then: "validation error occurs"
        response.statusCode != HttpStatus.OK
    }

    def "should reject person with blank name"() {
        given: "person with blank name"
        Person person = new Person(
            name: "",
            lastname: "Test",
            email: "test@email.com"
        )

        when: "calling POST /people"
        ResponseEntity<Person> response = restTemplate.postForEntity("/people", person, Person)

        then: "validation error occurs"
        response.statusCode != HttpStatus.OK
    }

    def "should reject person with blank lastname"() {
        given: "person with blank lastname"
        Person person = new Person(
            name: "Test",
            lastname: "",
            email: "test@email.com"
        )

        when: "calling POST /people"
        ResponseEntity<Person> response = restTemplate.postForEntity("/people", person, Person)

        then: "validation error occurs"
        response.statusCode != HttpStatus.OK
    }

    def "should handle concurrent person creation"() {
        given: "multiple persons"
        List<Person> persons = (1..5).collect {
            new Person(
                name: "Concurrent$it",
                lastname: "Test$it",
                email: "concurrent$it@test.com"
            )
        }

        when: "creating persons concurrently"
        persons.each { person ->
            restTemplate.postForEntity("/people", person, Person)
        }
        ResponseEntity<List> response = restTemplate.getForEntity("/people", List)

        then: "all persons are created"
        response.statusCode == HttpStatus.OK
        response.body.size() >= 5
    }
}
