package org.grails.datastore.rx.rest.config

import com.damnhandy.uri.template.UriTemplate
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
    UriTemplate uriTemplate

    /**
     * The content type returned by the server
     */
    String contentType = "application/json"

    /**
     * Sets a URI template
     *
     * @param uri The URI template
     */
    void setUri(String uri) {
        uriTemplate = UriTemplate.fromTemplate(uri)
    }

}
