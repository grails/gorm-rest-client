package org.grails.datastore.rx.rest.config

import groovy.transform.CompileStatic
import org.grails.datastore.mapping.config.Property

/**
 * Represents an attribute
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@CompileStatic
class Attribute extends Property {

    /**
     * The URI template this attribute maps to if it is an association
     */
    String uri
}
