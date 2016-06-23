package org.grails.datastore.rx.domain

import grails.gorm.annotation.Entity
import grails.gorm.rx.rest.RxRestEntity

/**
 * Created by graemerocher on 23/06/16.
 */
@Entity
class CommonTypes implements RxRestEntity<CommonTypes> {
    Long l
    Byte b
    Short s
    Boolean bool
    Integer i
    URL url
    Date date
    Calendar c
    BigDecimal bd
    BigInteger bi
    Double d
    Float f
    TimeZone tz
    Locale loc
    Currency cur
    byte[] ba
}
