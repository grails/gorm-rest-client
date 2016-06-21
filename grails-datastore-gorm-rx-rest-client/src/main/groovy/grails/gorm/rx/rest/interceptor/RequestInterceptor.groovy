package grails.gorm.rx.rest.interceptor

import grails.gorm.rx.RxEntity
import io.reactivex.netty.protocol.http.client.HttpClientRequest
import org.grails.datastore.rx.rest.RestEndpointPersistentEntity

/**
 * Intercept a {@link HttpClientRequest} and modify it if necessary prior to execution
 *
 * @author Graeme Rocher
 * @since 6.0
 */
interface RequestInterceptor {



    /**
     * The intercept method can modify the executing request. Many methods on the {@link HttpClientRequest} class return mutated copies so the mutated request should be
     * returned and the return type of this method.
     *
     * @param entity The entity being intercepted
     * @param instance The instance of the entity for which this request is being executed
     * @param request The request being intercepted
     *
     * @return The mutated or original request. An interceptor should never return null and should never perform a blocking operation.
     */
    HttpClientRequest intercept(RestEndpointPersistentEntity entity, RxEntity instance, HttpClientRequest request)
}
