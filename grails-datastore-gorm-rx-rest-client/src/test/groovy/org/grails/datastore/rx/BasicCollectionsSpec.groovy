package org.grails.datastore.rx

import groovy.json.JsonSlurper
import org.grails.datastore.rx.domain.Club

/**
 * Created by graemerocher on 24/06/16.
 */
class BasicCollectionsSpec extends RxGormSpec{
    @Override
    List<Class> getDomainClasses() {
        [Club]
    }

    void "Test encode basic collection types"() {

        when:"an entity with basic collection types is encoded"
        Club c = new Club(name: "Manchester United", nicknames: ['Red Devils', 'United'], squadNumbers: [Rooney:10])

        String jsonStr = c.toJson()
        def json = new JsonSlurper().parseText(jsonStr)

        then:"The results are correct"
        json.name == "Manchester United"
        json.nicknames == ['Red Devils', 'United']
        json.squadNumbers == [Rooney:10]
    }
}
