package org.grails.datastore.rx.rest.config

import groovy.transform.CompileStatic
import org.grails.datastore.mapping.config.AbstractGormMappingFactory
import org.grails.datastore.mapping.config.Property
import org.grails.datastore.mapping.model.MappingFactory

/**
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
class RestClientMappingFactory extends AbstractGormMappingFactory<Endpoint, Property> {
    @Override
    protected Class<Property> getPropertyMappedFormType() {
        return Property
    }

    @Override
    protected Class<Endpoint> getEntityMappedFormType() {
        return Endpoint
    }
}
