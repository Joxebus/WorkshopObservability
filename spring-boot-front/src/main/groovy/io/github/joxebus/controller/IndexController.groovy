package io.github.joxebus.controller

import groovy.util.logging.Slf4j
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

@Controller
@Slf4j
class IndexController {

    /**
     * This method return a String, by convention is the name
     * of the template less the extension
     * @param model the model returned to the view
     * @return index view placed at templates folder
     */
    @GetMapping("/")
    def index(Model model){
        log.info 'Calling index method'
        'index'
    }
}
