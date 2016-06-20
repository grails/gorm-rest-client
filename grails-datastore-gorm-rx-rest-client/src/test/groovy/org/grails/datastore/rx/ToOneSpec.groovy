package org.grails.datastore.rx

import grails.gorm.rx.proxy.ObservableProxy
import org.grails.datastore.rx.domain.Club
import org.grails.datastore.rx.domain.Person
import org.grails.datastore.rx.domain.Stadium
import rx.Observable

/**
 * Created by graemerocher on 20/06/16.
 */
class ToOneSpec extends RxGormSpec {
    @Override
    List<Class> getDomainClasses() {
        [Club, Person, Stadium]
    }

    void "Test that reading a hasOne association with a join query works correctly"() {
        given:"A canned response"
        def mock = client.expect {
            uri '/club/1'
            accept("application/json")
        }
        .respond {
            json {
                id 1
                name "Manchester United"
            }
        }
        mock.expect {
            uri '/club/1/stadium'
            accept("application/json")
        }
        .respond {
            json {
                id 1
                name "Old Trafford"
            }
        }

        when:"A get request is issued"
        Observable<Club> observable = Club.get(1, [fetch:[stadium:'eager']])
        Club c = observable.toBlocking().first()

        then:"The result is correct"
        mock.verify()
        c.name == "Manchester United"
        c.stadium != null
        c.stadium.name == "Old Trafford"
        !(c.stadium instanceof ObservableProxy)
    }

    void "Test that reading a hasOne association produces a proxy"() {
        given:"A canned response"
        def mock = client.expect {
            uri '/club/1'
            accept("application/json")
        }
        .respond {
            json {
                id 1
                name "Manchester United"
            }
        }

        when:"A get request is issued"
        Observable<Club> observable = Club.get(1)
        Club c = observable.toBlocking().first()

        then:"The result is correct"
        mock.verify()
        c.name == "Manchester United"
        c.stadium instanceof ObservableProxy

        when:"The proxy is initialized"

        mock = client.expect {
            uri '/club/1/stadium'
            accept("application/json")
        }
        .respond {
            json {
                id 1
                name "Old Trafford"
            }
        }

        Observable<Stadium> op = c.stadium.toObservable()
        Stadium s = op.toBlocking().first()

        then:"That result is correct"
        mock.verify()
        s.name == "Old Trafford"
    }

    void "Test that reading a regular to one association produces a proxy"() {
        given:"A canned response"
        def mock = client.expect {
            uri '/club/1'
            accept("application/json")
        }
        .respond {
            json {
                id 1
                name "Manchester United"
                owner 2
            }
        }

        when:"A get request is issued"
        Observable<Club> observable = Club.get(1)
        Club c = observable.toBlocking().first()

        then:"The result is correct"
        mock.verify()
        c.name == "Manchester United"
        c.owner instanceof ObservableProxy

        when:"The proxy is initialized"

        mock = client.expect {
            uri '/people/2'
            accept("application/json")
        }
        .respond {
            json {
                id 1
                name "Glazer"
            }
        }

        Observable<Person> op = c.owner.toObservable()
        Person p = op.toBlocking().first()

        then:"That result is correct"
        mock.verify()
        p.name == "Glazer"
    }
}
