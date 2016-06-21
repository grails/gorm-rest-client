package org.grails.datastore.rx.domain

import grails.gorm.annotation.Entity
import grails.gorm.rx.rest.RxRestEntity

/**
 * Created by graemerocher on 21/06/16.
 */
@Entity
class Product implements RxRestEntity<Product> {

    String name

    static mapping = {
        id name:"name", generator:"assigned"
    }
}
