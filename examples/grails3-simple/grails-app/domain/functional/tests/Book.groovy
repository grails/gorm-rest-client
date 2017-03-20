package functional.tests

import grails.rest.Resource


@Resource(uri="/books")
class Book  {
    Long id
    String title

    static constraints = {
        title blank:false
    }
}
