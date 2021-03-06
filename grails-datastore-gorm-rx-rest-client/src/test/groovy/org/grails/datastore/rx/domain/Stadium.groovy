package org.grails.datastore.rx.domain

import grails.gorm.annotation.Entity
import grails.gorm.rx.rest.RxRestEntity
import grails.http.MediaType

/**
 * Created by graemerocher on 20/06/16.
 */
@Entity
class Stadium implements RxRestEntity<Stadium> {
    String name

    static belongsTo = [club:Club]

    static mapping = {
        contentType MediaType.HAL_JSON
        uri "/club/{club}/stadium"
    }
}
