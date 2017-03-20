package functional.tests

import grails.gorm.rx.rest.RxRestEntity
import static grails.gorm.rx.rest.mapping.MappingBuilder.*

class BookClient implements RxRestEntity<BookClient> {
    Long id
    String title

    static mapping = endpoint {
        uri '/books{/id}'
    }
}
