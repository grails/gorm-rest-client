package org.grails.datastore.rx.rest;

import com.damnhandy.uri.template.UriTemplate;
import grails.gorm.rx.rest.interceptor.RequestInterceptor;
import grails.http.MediaType;
import groovy.transform.CompileStatic;
import org.grails.datastore.mapping.model.*;
import org.grails.datastore.mapping.model.types.Association;
import org.grails.datastore.rx.rest.config.Attribute;
import org.grails.datastore.rx.rest.config.Endpoint;

import java.beans.Introspector;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An entity that is mapped to a REST end point
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
public class RestEndpointPersistentEntity extends AbstractPersistentEntity<Endpoint> {
    protected final UriTemplate URI;
    protected final MediaType contentType;
    protected final Endpoint mappedForm;
    protected final RestEndpointClassMapping classMapping;
    protected final Map<String, UriTemplate> associationEndPoints = new LinkedHashMap<>();
    protected final RequestInterceptor[] interceptors;
    protected final Charset charset ;

    public RestEndpointPersistentEntity(Class javaClass, MappingContext context) {
        super(javaClass, context);
        this.mappedForm = (Endpoint)context.getMappingFactory().createMappedForm(RestEndpointPersistentEntity.this);
        this.classMapping = new RestEndpointClassMapping(this, context, mappedForm);
        Endpoint endpoint = getMapping().getMappedForm();
        this.URI = formulateURI(endpoint);
        this.contentType = endpoint.getContentType();
        this.interceptors = endpoint.getInterceptors();
        this.charset = endpoint.getCharset();
    }

    @Override
    public void initialize() {
        super.initialize();

        for(Association association : associations) {
            Attribute attr = (Attribute) association.getMapping().getMappedForm();

            UriTemplate uriTemplate = attr.getUriTemplate();
            if(uriTemplate != null) {
                associationEndPoints.put(association.getName(), uriTemplate);
            }
            else {
                Association inverseSide = association.getInverseSide();
                if(inverseSide != null) {
                    UriTemplate thisTemplate = getUriTemplate();

                    String baseTemplate = thisTemplate.getTemplate();
                    String identifierName = getMapping().getIdentifier().getIdentifierName()[0];
                    baseTemplate = baseTemplate.replace("{/"+identifierName+"}", "/{"+getDecapitalizedName()+"}");
                    associationEndPoints.put(association.getName(), UriTemplate.fromTemplate(baseTemplate + "/" + association.getName()));
                }
            }
        }
    }

    @Override
    public ClassMapping<Endpoint> getMapping() {
        return classMapping;
    }

    /**
     * Obtain a UriTemplate for the given association
     *
     * @param associationName The name of the association
     *
     * @return The template
     */
    public UriTemplate getAssociationTemplate(String associationName) {
        return this.associationEndPoints.get(associationName);
    }

    /**
     * @return The URI template for this endpoint
     */
    public UriTemplate getUriTemplate() {
        return URI;
    }

    /**
     * @return The content type of this endpoint
     */
    public MediaType getContentType() {
        return contentType;
    }


    /**
     * @return The charset to use
     */
    public Charset getCharset() {
        return this.charset;
    }
    /**
     * @return Any configured request interceptors
     */
    public RequestInterceptor[] getRequestInterceptors() {
        return this.interceptors;
    }

    private UriTemplate formulateURI(Endpoint endpoint) {
        UriTemplate uriTemplate = endpoint.getUriTemplate();
        if(uriTemplate != null) {
            return uriTemplate;
        }
        else {
            String identifierName = getMapping().getIdentifier().getIdentifierName()[0];
            return UriTemplate.fromTemplate( '/' + Introspector.decapitalize(getJavaClass().getSimpleName()) + "{/"+identifierName+"}" );
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
            return mappedForm;
        }
    }
}
