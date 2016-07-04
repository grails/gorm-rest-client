package grails.gorm.rx.rest.api

import grails.gorm.rx.api.RxGormInstanceOperations
import grails.http.client.builder.HttpClientRequestBuilder
import rx.Observable

/**
 * Methods on instances
 *
 * @author Graeme Rocher
 * @since 1.0
 */
interface RxRestGormInstanceOperations<D> extends RxGormInstanceOperations<D> {

    /**
     * @return Convert this object to a JSON string
     */
    String toJson(D instance)
    /**
     * Convert this object to JSON
     *
     * @param writer The target writer
     */
    void toJson(D instance, Writer writer)
    /**
     * Save an entity using the given closure to customize the request
     *
     * @param callable The callable
     * @return An observable
     */
    Observable<D> save(D instance, @DelegatesTo(HttpClientRequestBuilder) Closure callable)

    /**
     * Save an entity using the given closure to customize the request
     *
     * @param callable The callable
     * @return An observable
     */
    Observable<D> patch(D instance, @DelegatesTo(HttpClientRequestBuilder) Closure callable )
    /**
     * Save an entity using the given closure to customize the request
     *
     * @return An observable
     */
    Observable<D> patch(D instance)
    /**
     * Save an entity using the given closure to customize the request
     *
     * @param callable The callable
     * @return An observable
     */
    Observable<D> patch(D instance, Map arguments, @DelegatesTo(HttpClientRequestBuilder) Closure callable )

    /**
     * Save an entity using the given closure to customize the request
     *
     * @return An observable
     */
    Observable<D> patch(D instance, Map arguments)
    /**
     * Save an entity using the given closure to customize the request
     *
     * @param callable The callable
     * @return An observable
     */
    Observable<D> post(D instance, @DelegatesTo(HttpClientRequestBuilder) Closure callable)

    /**
     * Save an entity using the given closure to customize the request
     *
     * @return An observable
     */
    Observable<D> post(D instance)
    /**
     * Save an entity using the given closure to customize the request
     *
     * @param callable The callable
     * @return An observable
     */
    Observable<D> post(D instance, Map arguments, @DelegatesTo(HttpClientRequestBuilder) Closure callable)

    /**
     * Save an entity using the given closure to customize the request
     *
     * @return An observable
     */
    Observable<D> post(D instance, Map arguments)
    /**
     * Delete an entity using the given closure to customize the request
     *
     * @param callable The callable
     * @return An observable
     */
    Observable<Boolean> delete(D instance, @DelegatesTo(HttpClientRequestBuilder) Closure callable)

    /**
     * Inserts an entity using the given closure to customize the request
     *
     * @param callable The callable
     * @return An observable
     */
    Observable<D> insert(D instance, @DelegatesTo(HttpClientRequestBuilder) Closure callable)


    /**
     * Inserts an entity using the given closure to customize the request
     *
     * @param callable The callable
     * @return An observable
     */
    Observable<D> insert(D instance, Map arguments, @DelegatesTo(HttpClientRequestBuilder) Closure callable)

    /**
     * Saves an entity using the given closure to customize the request
     *
     * @param callable The callable
     * @return An observable
     */
    Observable<D> save(D instance, Map arguments, @DelegatesTo(HttpClientRequestBuilder) Closure callable)

    /**
     * Deletes an entity using the given closure to customize the request
     *
     * @param callable The callable
     * @return An observable
     */
    Observable<Boolean> delete(D instance, Map arguments, @DelegatesTo(HttpClientRequestBuilder) Closure callable)

}
