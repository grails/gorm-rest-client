package org.grails.datastore.rx.rest.paths

import groovy.transform.CompileStatic
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.rx.rest.config.RestClientMappingContext
import org.grails.datastore.rx.rest.config.RestEndpointPersistentEntity

/**
 * A Resource path resolver that resolves paths from the mapping context
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
class DefaultResourcePathResolver implements ResourcePathResolver {

    final RestClientMappingContext mappingContext

    DefaultResourcePathResolver(RestClientMappingContext mappingContext) {
        this.mappingContext = mappingContext
    }

    @Override
    String getPath(Class resource) {
        RestEndpointPersistentEntity entity = mappingContext.getPersistentEntity(resource.getName())
        return entity.getURI()
    }

    @Override
    String getPath(Class resource, Serializable id) {
        return getPath(resource) + "/$id"
    }
}
