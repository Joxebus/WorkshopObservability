package io.github.joxebus.service.impl

import io.github.joxebus.domain.Person
import io.github.joxebus.repository.PersonRepository
import io.github.joxebus.service.PersonService
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

    @Override
    List<Person> findAll() {
        personRepository.findAll()
    }

    @Override
    Person findById(Long id) {
        personRepository.findById(id).orElse(null)
    }

    @Override
    Person save(Person person) {
        def violations = validator.validate(person)
        if (!violations.empty) {
            throw new ConstraintViolationException("Person fields are incorrect", violations)
        }
        personRepository.save(person)
    }

    @Override
    boolean delete(Long id) {
        Person person = findById(id)
        if (person) {
            personRepository.delete(person)
            return true
        } else {
            return false
        }
    }
}
