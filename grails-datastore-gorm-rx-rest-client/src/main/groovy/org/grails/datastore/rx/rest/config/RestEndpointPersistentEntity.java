package org.grails.datastore.rx.rest.config;

import groovy.transform.CompileStatic;
import org.grails.datastore.mapping.keyvalue.mapping.config.Family;
import org.grails.datastore.mapping.keyvalue.mapping.config.KeyValuePersistentEntity;
import org.grails.datastore.mapping.model.*;

import java.beans.Introspector;

/**
 * An entity that is mapped to a REST end point
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
public class RestEndpointPersistentEntity extends AbstractPersistentEntity<Endpoint> {
    final String URI;
    final Endpoint mappedForm;
    final RestEndpointClassMapping classMapping;

    public RestEndpointPersistentEntity(Class javaClass, MappingContext context) {
        super(javaClass, context);
        this.mappedForm = (Endpoint)context.getMappingFactory().createMappedForm(RestEndpointPersistentEntity.this);
        this.classMapping = new RestEndpointClassMapping(this, context, mappedForm);
        this.URI = formulateURI();

    }

    @Override
    public ClassMapping<Endpoint> getMapping() {
        return classMapping;
    }

    public String getURI() {
        return URI;
    }

    private String formulateURI() {
        String uri = getMapping().getMappedForm().getUri();
        if(uri != null) {
            return uri;
        }
        else {
            return '/' + Introspector.decapitalize(getJavaClass().getSimpleName());
        }
    }

    static class RestEndpointClassMapping extends AbstractClassMapping<Endpoint> {
        final Endpoint mappedForm;

        public RestEndpointClassMapping(PersistentEntity entity, MappingContext context, Endpoint mappedForm) {
            super(entity, context);
            this.mappedForm = mappedForm;
        }

        @Override
        public Endpoint getMappedForm() {
            return (Endpoint) mappedForm;
        }
    }
}
