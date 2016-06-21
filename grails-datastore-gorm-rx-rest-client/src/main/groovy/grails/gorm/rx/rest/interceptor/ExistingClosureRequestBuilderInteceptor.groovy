package grails.gorm.rx.rest.interceptor

import grails.gorm.rx.RxEntity
import grails.http.client.builder.HttpClientRequestBuilder
import groovy.transform.CompileStatic
import io.reactivex.netty.protocol.http.client.HttpClientRequest
import org.grails.datastore.rx.rest.RestEndpointPersistentEntity

/**
 * Interceptor constructed from an existing closure
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
class ExistingClosureRequestBuilderInteceptor extends RequestBuilderInterceptor {

    final Closure callable

    ExistingClosureRequestBuilderInteceptor(@DelegatesTo(HttpClientRequestBuilder) Closure callable) {
        this.callable = callable
    }

    @Override
    Closure build(RestEndpointPersistentEntity entity, RxEntity instance, HttpClientRequest request) {
        return callable
    }
}
