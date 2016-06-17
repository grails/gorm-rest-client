package org.grails.datastore.rx.domain

import grails.gorm.annotation.Entity
import grails.gorm.rx.RxEntity

/**
 * Created by graemerocher on 15/06/16.
 */
@Entity
class Person implements RxEntity<Person> {

    String name
    Integer age
    Date dateOfBirth

    static mapping = {
        uri "/people{/id}"
    }
}
