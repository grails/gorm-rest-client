package org.grails.datastore.rx.domain

import grails.gorm.annotation.Entity
import grails.gorm.rx.rest.RxRestEntity

/**
 * Created by graemerocher on 20/06/16.
 */
@Entity
class Club implements RxRestEntity<Club> {

    String name

    // Maps to URI /person/1
    Person owner

    // Maps to URI /club/1/captain
    Person captain

    // Maps to URI /club/1/teams
    static hasMany = [teams: Team]

    // Maps to URI /club/1/stadium
    static hasOne = [stadium:Stadium]

    static mapping = {
        captain uri:"/club/{club}/captain"
    }
}
