package org.grails.datastore.rx.rest.config;

import com.damnhandy.uri.template.UriTemplate;
import groovy.transform.CompileStatic;
import org.grails.datastore.mapping.model.*;
import org.grails.datastore.mapping.model.types.Association;

import java.beans.Introspector;
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
    final UriTemplate URI;
    final String contentType;
    final Endpoint mappedForm;
    final RestEndpointClassMapping classMapping;
    final Map<String, UriTemplate> associationEndPoints = new LinkedHashMap<>();

    public RestEndpointPersistentEntity(Class javaClass, MappingContext context) {
        super(javaClass, context);
        this.mappedForm = (Endpoint)context.getMappingFactory().createMappedForm(RestEndpointPersistentEntity.this);
        this.classMapping = new RestEndpointClassMapping(this, context, mappedForm);
        Endpoint endpoint = getMapping().getMappedForm();
        this.URI = formulateURI(endpoint);
        this.contentType = endpoint.getContentType();
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
