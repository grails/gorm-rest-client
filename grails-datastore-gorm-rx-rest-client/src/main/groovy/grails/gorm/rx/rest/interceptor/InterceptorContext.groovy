package grails.gorm.rx.rest.interceptor

import grails.gorm.rx.RxEntity
import groovy.transform.CompileStatic
import org.grails.datastore.rx.rest.RestEndpointPersistentEntity
/**
 * A context object passed to each interceptor
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
class InterceptorContext {

    /**
     * The entity
     */
    final RestEndpointPersistentEntity entity
    /**
     * The instance
     */
    final RxEntity instance
    /**
     * Attributes shared across all interceptors
     */
    final Map<String,Object> attributes = [:]

    InterceptorContext(RestEndpointPersistentEntity entity, RxEntity instance = null) {
        this.entity = entity
        this.instance = instance
    }
}
