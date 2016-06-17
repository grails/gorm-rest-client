package org.grails.datastore.rx

import grails.http.HttpMethod
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
            method HttpMethod.POST
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
        p = p.save().toBlocking().first()

        then:"The result is correct"
        mock.verify()
        p.id == 1L
    }


    void "Test updating an existing entity produces a PUT request"() {
        given:"A canned response"
        def mock = client.expect {
            uri '/people/1'
            method HttpMethod.PUT
            json {
                name "Fred"
                age 10
                dateOfBirth "2006-07-09T00:00+0000"
            }
        }
        .respond {
            ok()
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
        p.id = 1L
        p = p.save().toBlocking().first()

        then:"The result is correct"
        mock.verify()
        p.id == 1L
    }
}
