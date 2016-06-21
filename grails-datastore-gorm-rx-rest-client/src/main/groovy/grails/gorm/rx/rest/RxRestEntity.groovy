package grails.gorm.rx.rest

import grails.gorm.rx.RxEntity
import grails.gorm.rx.rest.interceptor.ExistingClosureRequestBuilderInteceptor
import grails.gorm.rx.rest.interceptor.RequestBuilderInterceptor
import grails.http.client.builder.HttpClientRequestBuilder
import groovy.transform.CompileStatic
import io.reactivex.netty.protocol.http.client.HttpClientRequest
import org.grails.datastore.gorm.schemaless.DynamicAttributes
import org.grails.datastore.rx.rest.RestEndpointPersistentEntity
import org.grails.gorm.rx.api.RxGormEnhancer
import rx.Observable

/**
 * Represents an entity that is mapped as a REST entity in RxGORM
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@CompileStatic
trait RxRestEntity<D> implements RxEntity<D>, DynamicAttributes {

    /**
     * Retrieve an instance by id
     *
     * @param id The id of the instance
     * @return An observable
     */
    static Observable<D> get(Serializable id, @DelegatesTo(HttpClientRequestBuilder) Closure callable) {
        (Observable<D>)RxGormEnhancer.findStaticApi(this).get(id, [interceptor:createInterceptor(callable)])
    }
    /**
     * Retrieve an instance by id
     *
     * @param id The id of the instance
     * @return An observable
     */
    static Observable<D> get(Serializable id, Map arguments, @DelegatesTo(HttpClientRequestBuilder) Closure callable) {
        arguments.interceptor = createInterceptor(callable)
        (Observable<D>)RxGormEnhancer.findStaticApi(this).get(id, arguments)
    }

    /**
     * List all entities and return an observable
     *
     * @return An observable with all results
     */
    static Observable<List<D>> list(@DelegatesTo(HttpClientRequestBuilder) Closure callable) {
        (Observable<List<D>>)RxGormEnhancer.findStaticApi(this).list(interceptor:createInterceptor(callable))
    }

    /**
     * Finds all entities and return an observable
     *
     * @return An observable with all results
     */
    static Observable<D> findAll( @DelegatesTo(HttpClientRequestBuilder) Closure callable ) {
        (Observable<D>)RxGormEnhancer.findStaticApi(this).findAll(interceptor:createInterceptor(callable))
    }

    /**
     * Finds all entities and return an observable
     *
     * @return An observable with all results
     */
    static Observable<D> findAll(Map args,  @DelegatesTo(HttpClientRequestBuilder) Closure callable) {
        args.interceptor = createInterceptor(callable)
        RxGormEnhancer.findStaticApi(this).findAll(args)
    }

    /**
     * List all entities and return an observable
     *
     * @return An observable with all results
     */
    static Observable<List<D>> list(Map args, @DelegatesTo(HttpClientRequestBuilder) Closure callable) {
        args.interceptor = createInterceptor(callable)
        (Observable<List<D>>)RxGormEnhancer.findStaticApi(this).list(args)
    }
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

    private static RequestBuilderInterceptor createInterceptor(Closure callable) {
        return new ExistingClosureRequestBuilderInteceptor(callable)
    }
}
