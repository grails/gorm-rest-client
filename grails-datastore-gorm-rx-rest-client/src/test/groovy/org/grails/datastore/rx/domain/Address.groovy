package org.grails.datastore.rx.domain

import grails.gorm.annotation.Entity
import grails.gorm.rx.rest.RxRestEntity

/**
 * Created by graemerocher on 20/06/16.
 */
@Entity
class Address implements RxRestEntity<Address> {
    String street
    String postCode
}
