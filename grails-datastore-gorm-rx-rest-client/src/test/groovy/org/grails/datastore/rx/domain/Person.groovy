package org.grails.datastore.rx.domain

import grails.gorm.annotation.Entity
import grails.gorm.rx.RxEntity
import grails.gorm.rx.rest.RxRestEntity

/**
 * Created by graemerocher on 15/06/16.
 */
@Entity
class Person implements RxRestEntity<Person> {

    String name
    Integer age
    Date dateOfBirth
    Address address

    static mapping = {
        uri "/people{/id}"
    }
}
