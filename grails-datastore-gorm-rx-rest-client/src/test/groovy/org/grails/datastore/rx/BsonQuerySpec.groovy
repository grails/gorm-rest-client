package org.grails.datastore.rx

import org.grails.datastore.mapping.core.DatastoreUtils
import org.grails.datastore.rx.domain.Person
import org.grails.datastore.rx.rest.config.Settings
import org.grails.datastore.rx.rest.test.TestRxRestDatastoreClient
import rx.Observable

/**
 * Created by graemerocher on 22/06/16.
 */
class BsonQuerySpec extends RxGormSpec {

    @Override
    List<Class> getDomainClasses() {
        [Person]
    }

    @Override
    protected TestRxRestDatastoreClient createRestDatastoreClient(List<Class> classes) {
        def config = [(Settings.SETTING_QUERY_TYPE):"bson",
                      (Settings.SETTING_LOG_LEVEL) :"TRACE"]
        new TestRxRestDatastoreClient(DatastoreUtils.createPropertyResolver(config), classes as Class[])
    }

    void "Test the findAll method with criterion produces the right query"() {
        given:"A canned response"
        def mock = client.expect {
            def encoded = URLEncoder.encode('{"$or":[{"name":"Fred"},{"age":10}]}', "UTF-8")
            uri "/people?max=10&q=$encoded"
        }
        .respond {
            json( [
                    [
                            id: 1,
                            name: "Fred",
                            age: 10,
                            dateOfBirth: "2006-07-09T00:00+0000"]
                    ])
        }

        when:"A get request is issued"
        Observable<Person> observable = Person.where {
            name == "Fred" || age == 10
        }
        .max(10)
        .findAll()
        Person p = observable.toBlocking().first()

        then:"The result is correct"
        mock.verify()
        p.name == "Fred"
        p.age == 10
        dateFormat.format(p.dateOfBirth) == "2006-07-09T00:00+0000"
    }

    void "Test the findAll method with id criterion and other criterion produces the right query"() {
        given:"A canned response"
        println URLDecoder.decode("%7B%22id%22%3A1%2C%22age%22%3A10%7D", "UTF-8")
        def mock = client.expect {
            def encoded = URLEncoder.encode('{"id":1,"age":10}', "UTF-8")
            uri "/people/1?q=$encoded"
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
        Observable<Person> observable = Person.where {
            id == 1L && age == 10
        }.find()
        Person p = observable.toBlocking().first()

        then:"The result is correct"
        mock.verify()
        p.name == "Fred"
        p.age == 10
        dateFormat.format(p.dateOfBirth) == "2006-07-09T00:00+0000"
    }
}
