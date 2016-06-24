package grails.gorm.rx.rest.interceptor

import grails.gorm.rx.RxEntity
import grails.http.client.builder.HttpClientRequestBuilder
import io.reactivex.netty.protocol.http.client.HttpClientRequest
import io.reactivex.netty.protocol.http.client.HttpClientResponse
import org.grails.datastore.rx.rest.RestEndpointPersistentEntity
import rx.Observable

/**
 * Allows creating interceptors via builder syntax. Implementors should implement to the "build" method and call "buildRequest" providing the closure
 *
 *
 * @author Graeme Rocher
 * @since 6.0
 */
abstract class RequestBuilderInterceptor implements RequestInterceptor {

    @Override
    final Observable<HttpClientResponse> intercept(Observable<HttpClientResponse> request, InterceptorContext context) {
        if( !(request instanceof HttpClientRequest) ) {
            throw new IllegalStateException("Request cannot be written to from within an interceptor")
        }
        def builder = new HttpClientRequestBuilder((HttpClientRequest)request, context.entity.charset)

        def callable = build(request, context)
        callable.setDelegate(builder)
        callable.call()
        return builder.request
    }

    /**
     * The build method should be implemented by calling the buildRequest method passing in the closure that uses {@link HttpClientRequestBuilder} to alter the request
     *
     * @param entity The entity
     * @param instance The instance (null if a static call)
     * @param request The request
     *
     * @return The closure the performs the building
     */
    abstract Closure build(HttpClientRequest request, InterceptorContext context )

    protected Closure buildRequest(@DelegatesTo(HttpClientRequestBuilder) Closure callable) {
        return callable
    }
}
