package org.grails.datastore.rx.rest.config

import com.damnhandy.uri.template.UriTemplate
import groovy.transform.CompileStatic
import groovy.transform.builder.Builder
import groovy.transform.builder.SimpleStrategy
import org.grails.datastore.mapping.config.Property

/**
 * Represents an attribute
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@CompileStatic
@Builder(builderStrategy = SimpleStrategy, prefix = '')
class Attribute extends Property {

    /**
     * The URI template this attribute maps to if it is an association
     */
    UriTemplate uriTemplate

    /**
     * @param uri Sets the URI template
     */
    void setUri(String uri) {
        this.uriTemplate = UriTemplate.fromTemplate(uri)
    }

    /**
     * @param uri Sets the URI template
     */
    Attribute uri(String uri) {
        this.uriTemplate = UriTemplate.fromTemplate(uri)
        return this
    }
}
