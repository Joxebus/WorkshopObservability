package io.github.joxebus.controller

import org.springframework.ui.Model
import spock.lang.Specification
import spock.lang.Subject

class IndexControllerSpec extends Specification {

    Model model = Mock()

    @Subject
    IndexController indexController = new IndexController()

    def "should return index view"() {
        when: "calling index method"
        def result = indexController.index(model)

        then: "index view name is returned"
        result == "index"
    }

    def "should handle model correctly"() {
        when: "calling index method with model"
        def result = indexController.index(model)

        then: "model is passed through correctly"
        result == "index"
        0 * model._  // No model attributes should be added
    }
}
