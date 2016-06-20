package grails.gorm.rx.rest

import grails.gorm.rx.RxEntity
import org.grails.datastore.gorm.schemaless.DynamicAttributes

/**
 * Represents an entity that is mapped as a REST entity in RxGORM
 *
 * @author Graeme Rocher
 * @since 1.0
 */
trait RxRestEntity<D> implements RxEntity<D>, DynamicAttributes {
}
