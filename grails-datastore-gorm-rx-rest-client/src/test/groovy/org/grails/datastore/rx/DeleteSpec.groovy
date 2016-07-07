package org.grails.datastore.rx

import groovy.transform.NotYetImplemented
import org.grails.datastore.rx.domain.Person

/**
 * Created by graemerocher on 16/06/16.
 */
class DeleteSpec extends RxGormSpec {
    @Override
    List<Class> getDomainClasses() {
        [Person]
    }

    void "Test the delete method produces a delete request"() {
        given:"A canned response"
        def mock = client.expect {
            uri '/people/1'
            method "DELETE"
        }.respond {
            noContent()
        }

        when:"A get request is issued"
        def sw = new StringWriter()
        def date = new Date().parse('yyyy/MM/dd', '1973/07/09')

        Person p = new Person(name: "Fred", age: 10, dateOfBirth: date)
        p.id = 1L
        boolean deleted = p.delete().toBlocking().first()

        then:"The result is correct"
        mock.verify()
        deleted

        when:
        mock.reset()
        mock.expect {
            uri '/people/2'
            method "DELETE"
        }.respond {
            noContent()
        }

        p = new Person(name: "Fred", age: 10, dateOfBirth: date)
        p.id = 2L
        deleted = p.delete().toBlocking().first()

        then:"The result is correct"
        mock.verify()
        deleted

    }
}
