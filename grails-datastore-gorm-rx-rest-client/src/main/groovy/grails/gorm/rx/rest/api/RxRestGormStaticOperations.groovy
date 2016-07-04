package grails.gorm.rx.rest.api

import grails.gorm.rx.api.RxGormStaticOperations
import grails.gorm.rx.rest.RestDetachedCriteria
import grails.http.client.builder.HttpClientRequestBuilder
import rx.Observable

/**
 * Static methods for RxGORM for REST
 *
 * @author Graeme Rocher
 * @since 6.0
 *
 */
interface RxRestGormStaticOperations<D> extends RxGormStaticOperations<D> {
    /**
     * Construct and object from JSON
     *
     * @param json The JSON string
     * @return The object
     */
    Observable<D> fromJson(String json)

    /**
     * Construct and object from JSON
     *
     * @param reader The reader
     * @return The object
     */
    Observable<D> fromJson(Reader reader)

    /**
     * Retrieve an instance by id
     *
     * @param id The id of the instance
     * @return An observable
     */
    Observable<D> get(Serializable id, @DelegatesTo(HttpClientRequestBuilder) Closure callable)
    /**
     * Retrieve an instance by id
     *
     * @param id The id of the instance
     * @return An observable
     */
    Observable<D> get(Serializable id, Map arguments, @DelegatesTo(HttpClientRequestBuilder) Closure callable)

    /**
     * List all entities and return an observable
     *
     * @return An observable with all results
     */
    Observable<List<D>> list(@DelegatesTo(HttpClientRequestBuilder) Closure callable)

    /**
     * Finds all entities and return an observable
     *
     * @return An observable with all results
     */
    Observable<D> findAll( @DelegatesTo(HttpClientRequestBuilder) Closure callable )

    /**
     * Finds all entities and return an observable
     *
     * @return An observable with all results
     */
    Observable<D> findAll(Map args,  @DelegatesTo(HttpClientRequestBuilder) Closure callable)

    /**
     * List all entities and return an observable
     *
     * @return An observable with all results
     */
    Observable<List<D>> list(Map args, @DelegatesTo(HttpClientRequestBuilder) Closure callable)


    /**
     *
     * @param callable Callable closure containing detached criteria definition
     * @return The DetachedCriteria instance
     */
    RestDetachedCriteria<D> where(@DelegatesTo(RestDetachedCriteria) Closure callable)

    /**
     *
     * @param callable Callable closure containing detached criteria definition
     * @return The DetachedCriteria instance that is lazily initialized
     */
    RestDetachedCriteria<D> whereLazy(@DelegatesTo(RestDetachedCriteria) Closure callable)

    /**
     *
     * @param callable Callable closure containing detached criteria definition
     * @return The DetachedCriteria instance
     */
    RestDetachedCriteria<D> whereAny(@DelegatesTo(RestDetachedCriteria) Closure callable)
}