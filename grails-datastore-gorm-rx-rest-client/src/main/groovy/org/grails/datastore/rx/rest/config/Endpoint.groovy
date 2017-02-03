package org.grails.datastore.rx.rest.config

import com.damnhandy.uri.template.UriTemplate
import grails.gorm.rx.rest.interceptor.RequestInterceptor
import grails.http.MediaType
import groovy.transform.CompileStatic
import groovy.transform.builder.Builder
import groovy.transform.builder.SimpleStrategy
import org.grails.datastore.mapping.config.Entity
import org.grails.datastore.mapping.config.Property
import org.grails.datastore.mapping.reflect.ReflectionUtils

import java.nio.charset.Charset

/**
 * The configuration for mapping an entity to an endpoint
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
@Builder(builderStrategy = SimpleStrategy, prefix = '')
class Endpoint extends Entity {
    /**
     * The URI to the endpoint
     */
    UriTemplate uriTemplate

    /**
     * The content type returned by the server
     */
    MediaType contentType = MediaType.JSON

    /**
     * The default chartset to use
     */
    Charset charset = Charset.forName("UTF-8")

    /**
     * Any request interceptors
     */
    RequestInterceptor[] interceptors = new RequestInterceptor[0]

    /**
     * Sets a URI template
     *
     * @param uri The URI template
     */
    void setUri(CharSequence uri) {
        uriTemplate = UriTemplate.fromTemplate(uri.toString())
    }

    /**
     * Sets a URI template
     *
     * @param uri The URI template
     */
    Endpoint uri(CharSequence uri) {
        uriTemplate = UriTemplate.fromTemplate(uri.toString())
        return this
    }
    /**
     * @param contentType The content type
     */
    void setContentType(CharSequence contentType) {
        this.contentType = new MediaType(contentType.toString())
        Map<String,String> parameters = this.contentType.parameters
        if( parameters.containsKey(MediaType.CHARSET_PARAMETER) ) {
            this.charset = Charset.forName(parameters.get(MediaType.CHARSET_PARAMETER))
        }
    }

    /**
     * @param contentType The content type
     */
    Endpoint contentType(CharSequence contentType) {
        setContentType(contentType)
        return this
    }

    /**
     * @param mediaType The content type
     */
    Endpoint contentType(MediaType mediaType) {
        this.contentType = mediaType
        return this
    }
    /**
     * @param interceptorClasses The interceptor classes to set
     */
    void setInterceptors(Class<RequestInterceptor>...interceptorClasses) {
        RequestInterceptor[] interceptors = new RequestInterceptor[interceptorClasses.length]
        int i = 0
        for(cls in interceptorClasses) {
            interceptors[i++] = (RequestInterceptor)ReflectionUtils.instantiate(cls)
        }
        this.interceptors = interceptors
    }

    @Override
    Entity id(@DelegatesTo(Attribute.class) Closure identityConfig) {
        return super.id(identityConfig)
    }

    @Override
    Entity version(@DelegatesTo(Attribute.class) Closure versionConfig) {
        return super.version(versionConfig)
    }

    @Override
    Entity property(String name, @DelegatesTo(Attribute.class) Closure propertyConfig) {
        return super.property(name, propertyConfig)
    }

    @Override
    Attribute property(@DelegatesTo(Attribute.class) Closure propertyConfig) {
        return (Attribute) super.property(propertyConfig)
    }

    @Override
    protected Attribute newProperty() {
        return new Attribute()
    }
}
