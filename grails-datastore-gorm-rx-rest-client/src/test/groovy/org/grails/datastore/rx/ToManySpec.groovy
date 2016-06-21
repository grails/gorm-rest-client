package org.grails.datastore.rx

import grails.gorm.rx.collection.RxPersistentCollection
import grails.http.MediaType
import org.grails.datastore.rx.domain.Club
import org.grails.datastore.rx.domain.Person
import org.grails.datastore.rx.domain.Stadium
import org.grails.datastore.rx.domain.Team
import rx.Observable

/**
 * Created by graemerocher on 21/06/16.
 */
class ToManySpec extends RxGormSpec {
    @Override
    List<Class> getDomainClasses() {
        [Club, Team, Person, Stadium]
    }

    void "Test a hasMany association lazy loads from the correct URI"() {
        given:"A canned response"
        def mock = client.expect {
            uri '/club/1'
            accept(MediaType.HAL_JSON)
        }
        .respond {
            json {
                id 1
                name "Manchester United"
            }
        }
        mock.expect {
            uri '/club/1/teams'
            accept(MediaType.HAL_JSON)
        }
        .respond {
            json {
                _embedded {
                    teams(
                        [
                            [id:1L,name: "First Team"],
                            [id:2L,name: "Under 21s"]
                        ]
                    )
                }
                totalCount 2
            }
        }

        when:"A get request is issued"
        Observable<Club> observable = Club.get(1)
        Club c = observable.toBlocking().first()

        then:"The teams are a lazy collection"
        c.teams instanceof RxPersistentCollection
        c.teams.size() == 2
        c.teams*.name.sort() == ["First Team", "Under 21s"]
        mock.verify()
    }
}
