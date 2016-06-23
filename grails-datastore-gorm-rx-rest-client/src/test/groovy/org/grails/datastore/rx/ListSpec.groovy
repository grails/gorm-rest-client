package org.grails.datastore.rx

import org.grails.datastore.rx.domain.Person
import rx.Observable

/**
 * Created by graemerocher on 23/06/16.
 */
class ListSpec extends RxGormSpec {
    @Override
    List<Class> getDomainClasses() {
        [Person]
    }

    void "Test the list method with a custom URI"() {
        given:"A canned response"
        def mock = client.expect {
            uri '/people/recent'
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
        Observable<List<Person>> observable = Person.list(uri:"/people/recent")
        List<Person> people = observable.toBlocking().first()

        then:"The result is correct"
        mock.verify()
        people.size() == 2
        people[0].id == 1
        people[0].name == "Fred"
        people[0].age == 10
        people[1].id == 2
        people[1].name == "Joe"
        people[1].age == 12
    }
}
