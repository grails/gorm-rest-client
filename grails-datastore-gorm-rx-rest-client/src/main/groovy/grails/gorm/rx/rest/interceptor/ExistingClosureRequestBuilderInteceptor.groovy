package grails.gorm.rx.rest.interceptor

import grails.http.client.builder.HttpClientRequestBuilder
import groovy.transform.CompileStatic
import io.reactivex.netty.protocol.http.client.HttpClientRequest

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
    Closure build(HttpClientRequest request, InterceptorContext context) {
        return callable
    }
}
