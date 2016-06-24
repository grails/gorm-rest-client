package grails.gorm.rx.rest.interceptor

import io.reactivex.netty.protocol.http.client.HttpClientRequest
import io.reactivex.netty.protocol.http.client.HttpClientResponse
import rx.Observable

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
     * @param request The request
     * @param context The interceptor context
     *
     * @return The mutated or original request. An interceptor should never return null and should never perform a blocking operation.
     */
    Observable<HttpClientResponse> intercept(Observable<HttpClientResponse> request, InterceptorContext context)
}
