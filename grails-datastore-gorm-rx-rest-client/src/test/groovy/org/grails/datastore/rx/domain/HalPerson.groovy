package org.grails.datastore.rx.domain

import grails.gorm.annotation.Entity
import grails.gorm.rx.RxEntity
import grails.http.MediaType

/**
 * Created by graemerocher on 17/06/16.
 */
@Entity
class HalPerson implements RxEntity<Person> {

    String name
    Integer age
    Date dateOfBirth

    static mapping = {
        uri "/people{/id}"
        contentType MediaType.HAL_JSON
    }
}
