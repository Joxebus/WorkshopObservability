package io.github.joxebus.controller

import io.github.joxebus.domain.Person
import io.github.joxebus.service.PersonService
import groovy.util.logging.Slf4j
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

    @CrossOrigin(origins = "http://localhost:8080")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    List<Person> restList() {
        log.info "Get list of persons as JSON"
        personService.findAll()
    }

    @CrossOrigin(origins = "http://localhost:8080")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    Person restSave(@RequestBody Person person) {
        log.info "Saving person $person"
        personService.save(person)
    }

    @CrossOrigin(origins = "http://localhost:8080")
    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    Person restUpdate(@RequestBody Person person) {
        log.info "Updating person $person"
        personService.save(person)
    }

    @CrossOrigin(origins = "http://localhost:8080")
    @GetMapping(value = '/{id}',
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    Person restGetPerson(@PathVariable Long id) {
        log.info "Get person with id: $id"
        personService.findById(id)
    }

    @CrossOrigin(origins = "http://localhost:8080")
    @DeleteMapping(value = '/{id}',
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<?> restDeletePerson(@PathVariable Long id) {
        log.info "Delete person with id: $id"
        HttpStatus status = null;
        try {
            if (personService.delete(id)) {
                log.debug("SUCCESS")
                status = HttpStatus.OK
            } else {
                log.error("ERROR")
                status = HttpStatus.UNPROCESSABLE_ENTITY
            }

        } catch (Error e) {
            log.error(e.getMessage())
            status = HttpStatus.BAD_REQUEST
        }
        return new ResponseEntity<String>(status)
    }
}
