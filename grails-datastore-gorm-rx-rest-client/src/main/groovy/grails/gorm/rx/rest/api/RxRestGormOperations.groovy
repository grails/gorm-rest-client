package grails.gorm.rx.rest.api

import grails.gorm.rx.api.RxGormOperations
import grails.http.client.builder.HttpClientRequestBuilder
import rx.Observable

/**
 * Methods to be performed on entities
 *
 * @author Graeme Rocher
 * @since 6.0
 */
interface RxRestGormOperations<D> extends RxGormOperations<D> {
    /**
     * @return Convert this object to a JSON string
     */
    String toJson()
    /**
     * Convert this object to JSON
     *
     * @param writer The target writer
     */
    void toJson(Writer writer)
    /**
     * Save an entity using the given closure to customize the request
     *
     * @param callable The callable
     * @return An observable
     */
    Observable<D> save(@DelegatesTo(HttpClientRequestBuilder) Closure callable)

    /**
     * Save an entity using the given closure to customize the request
     *
     * @param callable The callable
     * @return An observable
     */
    Observable<D> patch(@DelegatesTo(HttpClientRequestBuilder) Closure callable )
    /**
     * Save an entity using the given closure to customize the request
     *
     * @return An observable
     */
    Observable<D> patch()
    /**
     * Save an entity using the given closure to customize the request
     *
     * @param callable The callable
     * @return An observable
     */
    Observable<D> patch(Map arguments, @DelegatesTo(HttpClientRequestBuilder) Closure callable )

    /**
     * Save an entity using the given closure to customize the request
     *
     * @return An observable
     */
    Observable<D> patch(Map arguments)
    /**
     * Save an entity using the given closure to customize the request
     *
     * @param callable The callable
     * @return An observable
     */
    Observable<D> post(@DelegatesTo(HttpClientRequestBuilder) Closure callable)

    /**
     * Save an entity using the given closure to customize the request
     *
     * @return An observable
     */
    Observable<D> post()
    /**
     * Save an entity using the given closure to customize the request
     *
     * @param callable The callable
     * @return An observable
     */
    Observable<D> post(Map arguments, @DelegatesTo(HttpClientRequestBuilder) Closure callable)

    /**
     * Save an entity using the given closure to customize the request
     *
     * @return An observable
     */
    Observable<D> post(Map arguments)
    /**
     * Delete an entity using the given closure to customize the request
     *
     * @param callable The callable
     * @return An observable
     */
    Observable<Boolean> delete(@DelegatesTo(HttpClientRequestBuilder) Closure callable)

    /**
     * Inserts an entity using the given closure to customize the request
     *
     * @param callable The callable
     * @return An observable
     */
    Observable<D> insert(@DelegatesTo(HttpClientRequestBuilder) Closure callable)


    /**
     * Inserts an entity using the given closure to customize the request
     *
     * @param callable The callable
     * @return An observable
     */
    Observable<D> insert(Map arguments, @DelegatesTo(HttpClientRequestBuilder) Closure callable)

    /**
     * Saves an entity using the given closure to customize the request
     *
     * @param callable The callable
     * @return An observable
     */
    Observable<D> save(Map arguments, @DelegatesTo(HttpClientRequestBuilder) Closure callable)

    /**
     * Deletes an entity using the given closure to customize the request
     *
     * @param callable The callable
     * @return An observable
     */
    Observable<Boolean> delete(Map arguments, @DelegatesTo(HttpClientRequestBuilder) Closure callable)
}
