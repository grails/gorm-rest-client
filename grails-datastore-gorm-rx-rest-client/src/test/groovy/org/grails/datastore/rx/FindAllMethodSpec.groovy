package org.grails.datastore.rx

import org.grails.datastore.rx.domain.Person
import rx.Observable
import spock.lang.Ignore

/**
 * Created by graemerocher on 15/06/16.
 */
@Ignore
class FindAllMethodSpec extends RxGormSpec {
    @Override
    List<Class> getDomainClasses() {
        [Person]
    }

    void "Test the findAll method returns all objects"() {
        given:"A canned response"
        def mock = client.expect {
            uri '/person'
        }
        .respond {
            json( [
                [
                id: 1,
                name: "Fred",
                age: 10,
                dateOfBirth: "2006-07-09T00:00+0000"],
                [
                id: 2,
                name: "Joe",
                age: 12,
                dateOfBirth: "2004-07-09T00:00+0000"],
            ] )
        }

        when:"A get request is issued"
        Observable<Person> observable = Person.findAll()
        List<Person> people = observable.toList().toBlocking()

        then:"The result is correct"
        mock.verify()
        people.size() == 2
    }
}
