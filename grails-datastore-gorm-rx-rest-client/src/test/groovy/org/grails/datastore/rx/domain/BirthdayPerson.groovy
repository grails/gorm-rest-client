package org.grails.datastore.rx.domain

import grails.gorm.annotation.Entity
import grails.gorm.rx.rest.RxRestEntity

/**
 * Created by graemerocher on 24/06/16.
 */
@Entity
class BirthdayPerson implements RxRestEntity<BirthdayPerson> {
    String name
    Birthday birthday
}
