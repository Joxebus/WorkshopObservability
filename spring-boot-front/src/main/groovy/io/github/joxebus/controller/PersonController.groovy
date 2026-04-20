package io.github.joxebus.controller

import io.github.joxebus.domain.Person
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.client.RestTemplate
import org.springframework.web.servlet.mvc.support.RedirectAttributes

@Controller
@RequestMapping('/people')
@Slf4j
class PersonController {

    @Autowired
    RestTemplate restTemplate

    /**
     * Returns a model and view to display the information
     * @param model contains the info to render
     * @return view
     */
    @GetMapping
    def list(Model model){
        log.info "Calling list method"
        try {
            List listOfPeople = restTemplate.getForObject("/people", List)
            model.addAttribute('listOfPeople', listOfPeople)
        } catch(Exception e) {
            model.addAttribute("error", "Can't load data ${e.message}")
            log.error (e.message, e)
        }

        'person/list'
    }

    @GetMapping("/new")
    def newPerson(Model model){
        log.info 'Calling new method'
        model.addAttribute("person", new Person())
        'person/create'
    }

    @PostMapping("/create")
    def createPerson(@ModelAttribute Person person, Model model, RedirectAttributes redirAttrs){
        log.info 'Calling create method'
        try{
            HttpHeaders requestHeaders = new HttpHeaders()
            requestHeaders.setContentType(MediaType.APPLICATION_JSON)
            HttpEntity<Person> data = new HttpEntity<>(person, requestHeaders)
            def response = restTemplate.postForEntity("/people",data, Person)
            if(response.statusCode == HttpStatus.OK){
                redirAttrs.addFlashAttribute("message", "Successfuly added ${response.body.name}")
                return 'redirect:/people/'
            } else{
                model.addAttribute("person", response.body)
                model.addAttribute("error", "Verify your information")
                return 'person/create'
            }

        } catch(Exception e) {
            model.addAttribute("person", person)
            model.addAttribute("error", "Verify your information")
            return 'person/create'
        }
    }

    @GetMapping("/delete/{id}")
    def delete(@PathVariable("id") Long id, RedirectAttributes redirAttrs){
        log.info "Calling delete for person $id"
        try{
            restTemplate.delete("/people/$id")
            redirAttrs.addFlashAttribute("message", "Successfuly deleted person wit id = [${id}]")
        } catch(Exception e) {
            redirAttrs.addFlashAttribute("error", "Verify your information")
        }
        return 'redirect:/people/'
    }

}
