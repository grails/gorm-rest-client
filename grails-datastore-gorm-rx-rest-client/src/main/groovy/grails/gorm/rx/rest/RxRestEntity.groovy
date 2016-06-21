package grails.gorm.rx.rest

import grails.gorm.rx.RxEntity
import grails.gorm.rx.rest.interceptor.ExistingClosureRequestBuilderInteceptor
import grails.gorm.rx.rest.interceptor.RequestBuilderInterceptor
import grails.http.client.builder.HttpClientRequestBuilder
import io.reactivex.netty.protocol.http.client.HttpClientRequest
import org.grails.datastore.gorm.schemaless.DynamicAttributes
import org.grails.datastore.rx.rest.RestEndpointPersistentEntity
import rx.Observable

/**
 * Represents an entity that is mapped as a REST entity in RxGORM
 *
 * @author Graeme Rocher
 * @since 1.0
 */
trait RxRestEntity<D> implements RxEntity<D>, DynamicAttributes {

    /**
     * Save an entity using the given closure to customize the request
     *
     * @param callable The callable
     * @return An observable
     */
    Observable<D> save(@DelegatesTo(HttpClientRequestBuilder) Closure callable) {
        return RxEntity.super.save(interceptor: createInterceptor(callable))
    }

    /**
     * Delete an entity using the given closure to customize the request
     *
     * @param callable The callable
     * @return An observable
     */
    Observable<Boolean> delete(@DelegatesTo(HttpClientRequestBuilder) Closure callable) {
        return RxEntity.super.delete(interceptor: createInterceptor(callable))
    }


    /**
     * Inserts an entity using the given closure to customize the request
     *
     * @param callable The callable
     * @return An observable
     */
    Observable<D> insert(@DelegatesTo(HttpClientRequestBuilder) Closure callable) {
        return RxEntity.super.insert(interceptor: createInterceptor(callable))
    }

    /**
     * Inserts an entity using the given closure to customize the request
     *
     * @param callable The callable
     * @return An observable
     */
    Observable<D> insert(Map<String, Object> arguments, @DelegatesTo(HttpClientRequestBuilder) Closure callable) {
        arguments.interceptor = createInterceptor(callable)
        return RxEntity.super.insert(arguments)
    }

    /**
     * Saves an entity using the given closure to customize the request
     *
     * @param callable The callable
     * @return An observable
     */
    Observable<D> save(Map<String, Object> arguments, @DelegatesTo(HttpClientRequestBuilder) Closure callable) {
        arguments.interceptor = createInterceptor(callable)
        return RxEntity.super.save(arguments)

    }

    /**
     * Deletes an entity using the given closure to customize the request
     *
     * @param callable The callable
     * @return An observable
     */
    Observable<Boolean> delete(Map<String, Object> arguments, @DelegatesTo(HttpClientRequestBuilder) Closure callable) {
        arguments.interceptor = createInterceptor(callable)
        return RxEntity.super.delete(arguments)
    }

    private RequestBuilderInterceptor createInterceptor(Closure callable) {
        return new ExistingClosureRequestBuilderInteceptor(callable)
    }
}
