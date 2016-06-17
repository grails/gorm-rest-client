package org.grails.datastore.rx.rest.config;

import com.damnhandy.uri.template.UriTemplate;
import groovy.transform.CompileStatic;
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
    final UriTemplate URI;
    final String contentType;
    final Endpoint mappedForm;
    final RestEndpointClassMapping classMapping;

    public RestEndpointPersistentEntity(Class javaClass, MappingContext context) {
        super(javaClass, context);
        this.mappedForm = (Endpoint)context.getMappingFactory().createMappedForm(RestEndpointPersistentEntity.this);
        this.classMapping = new RestEndpointClassMapping(this, context, mappedForm);
        Endpoint endpoint = getMapping().getMappedForm();
        this.URI = formulateURI(endpoint);
        this.contentType = endpoint.getContentType();
    }

    @Override
    public ClassMapping<Endpoint> getMapping() {
        return classMapping;
    }

    public UriTemplate getUriTemplate() {
        return URI;
    }

    public String getContentType() {
        return contentType;
    }

    private UriTemplate formulateURI(Endpoint endpoint) {
        UriTemplate uriTemplate = endpoint.getUriTemplate();
        if(uriTemplate != null) {
            return uriTemplate;
        }
        else {
            return UriTemplate.fromTemplate( '/' + Introspector.decapitalize(getJavaClass().getSimpleName()) + "{/id}" );
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
