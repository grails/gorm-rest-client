package org.grails.datastore.rx.rest.config

import groovy.transform.CompileStatic
import org.grails.datastore.mapping.config.Entity

/**
 * The configuration for mapping an entity to an endpoint
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
class Endpoint extends Entity {
    /**
     * The URI to the endpoint
     */
    String uri

    /**
     * The content type returned by the server
     */
    String contentType = "application/json"
}
