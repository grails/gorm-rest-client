package functional.tests

import grails.gorm.rx.rest.RxRestEntity
import grails.test.mixin.integration.Integration
import org.grails.datastore.rx.RxDatastoreClient
import org.grails.datastore.rx.rest.RxRestDatastoreClient
import org.springframework.boot.test.WebIntegrationTest
import spock.lang.AutoCleanup
import spock.lang.Specification

/**
 * Created by graemerocher on 19/07/2016.
 */
@Integration
@WebIntegrationTest(randomPort = false)
class BookClientSpec extends Specification {

    @AutoCleanup RxDatastoreClient datastoreClient
    def setup() {
        datastoreClient = new RxRestDatastoreClient(BookClientClient)
    }

    void "Test list books"() {
        expect:
        BookClientClient.list().toBlocking().first().size() == 1
    }


}

class BookClientClient implements RxRestEntity<BookClientClient> {
    Long id
    String title

    static mapping = {
        uri '/client/books{/id}'
    }

}
