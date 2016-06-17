package org.grails.datastore.rx

import grails.http.MediaType
import org.grails.datastore.rx.domain.HalPerson
import org.grails.datastore.rx.domain.Person
import rx.Observable

/**
 * Created by graemerocher on 15/06/16.
 */
class GetSpec extends RxGormSpec {

    @Override
    List<Class> getDomainClasses() {
        [Person, HalPerson]
    }


    void "Test get an entity using a GET request that returns HAL"() {
        given:"A canned response"
        def mock = client.expect {
            uri '/people/1'
            accept(MediaType.HAL_JSON)
        }
        .respond {
            contentType(MediaType.HAL_JSON)
            json {
                _links {
                    self {
                        href "/orders"
                    }
                }
                id 1
                name "Fred"
                age 10
                dateOfBirth "2006-07-09T00:00+0000"
            }
        }

        when:"A get request is issued"
        Observable<Person> observable = HalPerson.get(1)
        HalPerson p = observable.toBlocking().first()

        then:"The result is correct"
        mock.verify()
        p.name == "Fred"
        p.age == 10
        dateFormat.format(p.dateOfBirth) == "2006-07-09T00:00+0000"
    }

    void "Test get an entity using a GET request"() {
        given:"A canned response"
        def mock = client.expect {
            uri '/people/1'
            accept("application/json")
        }
        .respond {
            json {
                id 1
                name "Fred"
                age 10
                dateOfBirth "2006-07-09T00:00+0000"
            }
        }

        when:"A get request is issued"
        Observable<Person> observable = Person.get(1)
        Person p = observable.toBlocking().first()

        then:"The result is correct"
        mock.verify()
        p.name == "Fred"
        p.age == 10
        dateFormat.format(p.dateOfBirth) == "2006-07-09T00:00+0000"
    }
}
