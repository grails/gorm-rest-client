package functional.tests

import grails.test.mixin.integration.Integration
import org.grails.datastore.rx.RxDatastoreClient
import org.grails.datastore.rx.rest.RxRestDatastoreClient
import org.springframework.boot.test.WebIntegrationTest
import spock.lang.AutoCleanup
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@Integration
@WebIntegrationTest(randomPort = false)
class BookSpec extends Specification {

    @AutoCleanup RxDatastoreClient datastoreClient
    def setup() {
        datastoreClient = new RxRestDatastoreClient(BookClient)
    }

    void "Test list books"() {
        expect:
        BookClient.list().toBlocking().first().size() == 1
    }

    void "Test save book"() {
        when:
        BookClient book = new BookClient(title:"The Stand").save().toBlocking().first()

        then:"The book is correct"
        book.title == "The Stand"
        book.id != null
        BookClient.list().toBlocking().first().size() == 2

        when:"The book is updated"
        book.title = "The Shining"
        book.save().toBlocking().first()

        book = BookClient.get(book.id).toBlocking().first()

        then:"It is updated"
        book.title == "The Shining"
        BookClient.list().toBlocking().first().size() == 2

        when:"The book is deleted"
        boolean deleted = book.delete().toBlocking().first()

        then:"It was deleted"
        deleted
        BookClient.list().toBlocking().first().size() == 1
    }

}
