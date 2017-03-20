package functional.tests

import functional.tests.*

class BootStrap {

    def init = { servletContext ->
        new Book(title: "It").save(flush:true)
    }
    def destroy = {
    }
}
