package functional.tests

import grails.gorm.rx.rest.RxRestEntity

class BookClient implements RxRestEntity<BookClient> {
    Long id
    String title

    static mapping = {
        uri '/books{/id}'
    }
}
