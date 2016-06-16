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

    @NotYetImplemented
    void "Test the delete method produces a delete request"() {

    }
}
