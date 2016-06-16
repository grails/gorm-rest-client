package org.grails.datastore.rx

import org.grails.datastore.rx.domain.Person
import rx.Observable

/**
 * Created by graemerocher on 16/06/16.
 */
class SaveSpec extends RxGormSpec {
    @Override
    List<Class> getDomainClasses() {
        [Person]
    }

    void "Test saving a new entity produces a POST request"() {
        given:"A canned response"
        def mock = client.expect {
            uri '/people'
            method "POST"
            json {
                name "Fred"
                age 10
                dateOfBirth "2006-07-09T00:00+0000"
            }
        }
        .respond {
            created()
            json {
                id 1
                name "Fred"
                age 10
                dateOfBirth "2006-07-09T00:00+0000"
            }
        }

        when:"A get request is issued"
        def sw = new StringWriter()
        def date = new Date().parse('yyyy/MM/dd', '1973/07/09')

        Person p = new Person(name: "Fred", age: 10, dateOfBirth: date)
        p.save().toBlocking().first()

        then:"The result is correct"
        mock.verify()
        p.id == 1L
    }
}
