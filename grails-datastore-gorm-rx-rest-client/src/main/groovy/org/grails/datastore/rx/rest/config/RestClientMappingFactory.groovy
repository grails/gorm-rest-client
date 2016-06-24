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
class RestClientMappingFactory extends AbstractGormMappingFactory<Endpoint, Attribute> {

    final Set<Class> additionalSimpleTypes = []

    @Override
    protected Class<Attribute> getPropertyMappedFormType() {
        return Attribute
    }

    @Override
    protected Class<Endpoint> getEntityMappedFormType() {
        return Endpoint
    }

    @Override
    boolean isSimpleType(Class propType) {
        return additionalSimpleTypes.contains(propType) || super.isSimpleType(propType)
    }

    void addSimpleType(Class type) {
        additionalSimpleTypes.add(type)
    }
}
