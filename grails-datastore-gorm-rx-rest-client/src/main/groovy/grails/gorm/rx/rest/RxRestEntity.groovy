package grails.gorm.rx.rest

import grails.gorm.rx.RxEntity
import grails.gorm.rx.api.RxGormAllOperations
import grails.gorm.rx.rest.api.RxRestGormAllOperations
import grails.gorm.rx.rest.api.RxRestGormOperations
import grails.gorm.rx.rest.interceptor.ExistingClosureRequestBuilderInteceptor
import grails.gorm.rx.rest.interceptor.RequestBuilderInterceptor
import grails.http.HttpMethod
import grails.http.client.builder.HttpClientRequestBuilder
import groovy.transform.CompileStatic
import org.bson.codecs.Codec
import org.grails.datastore.bson.json.JsonWriter
import org.grails.datastore.gorm.schemaless.DynamicAttributes
import org.grails.datastore.rx.bson.codecs.RxBsonPersistentEntityCodec
import org.grails.datastore.rx.rest.RxRestDatastoreClient
import org.grails.datastore.rx.rest.api.RxRestGormInstanceApi
import org.grails.datastore.rx.rest.api.RxRestGormStaticApi
import org.grails.datastore.rx.rest.config.Settings
import org.grails.gorm.rx.api.RxGormEnhancer
import rx.Observable

/**
 * Represents an entity that is mapped as a REST entity in RxGORM
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@CompileStatic
trait RxRestEntity<D> implements RxEntity<D>, DynamicAttributes, RxRestGormOperations<D> {

    /**
     * @return Convert this object to a JSON string
     */
    String toJson() {
        def writer = new StringWriter()
        toJson(writer)
        return writer.toString()
    }

    /**
     * Convert this object to JSON
     *
     * @param writer The target writer
     */
    void toJson(Writer writer) {
        RxRestGormStaticApi restStaticApi = currentRestStaticApi()

        RxRestDatastoreClient client = (RxRestDatastoreClient) restStaticApi.datastoreClient
        Codec<Object> codec = (Codec<Object>)client.codecRegistry.get(getClass())
        codec.encode(new JsonWriter(writer),(Object)this, RxBsonPersistentEntityCodec.DEFAULT_ENCODER_CONTEXT)
    }

    /**
     * Save an entity using the given closure to customize the request
     *
     * @param callable The callable
     * @return An observable
     */
    Observable<D> save(@DelegatesTo(HttpClientRequestBuilder) Closure callable) {
        currentRestInstanceApi().save((D)this, callable)
    }

    /**
     * Save an entity using the given closure to customize the request
     *
     * @param callable The callable
     * @return An observable
     */
    Observable<D> patch(@DelegatesTo(HttpClientRequestBuilder) Closure callable = null) {
        currentRestInstanceApi().patch((D)this, callable)
    }

    /**
     * Save an entity using the given closure to customize the request
     *
     * @param callable The callable
     * @return An observable
     */
    Observable<D> patch(Map arguments, @DelegatesTo(HttpClientRequestBuilder) Closure callable = null) {
        currentRestInstanceApi().patch((D)this, arguments, callable)
    }

    /**
     * Save an entity using the given closure to customize the request
     *
     * @param callable The callable
     * @return An observable
     */
    Observable<D> post(@DelegatesTo(HttpClientRequestBuilder) Closure callable = null) {
        currentRestInstanceApi().post((D)this, [:], callable)
    }

    /**
     * Save an entity using the given closure to customize the request
     *
     * @param callable The callable
     * @return An observable
     */
    Observable<D> post(Map arguments, @DelegatesTo(HttpClientRequestBuilder) Closure callable = null) {
        currentRestInstanceApi().post((D)this, arguments, callable)
    }
    /**
     * Delete an entity using the given closure to customize the request
     *
     * @param callable The callable
     * @return An observable
     */
    Observable<Boolean> delete(@DelegatesTo(HttpClientRequestBuilder) Closure callable) {
        return currentRestInstanceApi().delete((D)this, [interceptor: createInterceptor(callable)])
    }

    /**
     * Inserts an entity using the given closure to customize the request
     *
     * @param callable The callable
     * @return An observable
     */
    Observable<D> insert(@DelegatesTo(HttpClientRequestBuilder) Closure callable) {
        return currentRestInstanceApi().insert((D)this, [interceptor: createInterceptor(callable)])
    }


    /**
     * Inserts an entity using the given closure to customize the request
     *
     * @param callable The callable
     * @return An observable
     */
    Observable<D> insert(Map arguments, @DelegatesTo(HttpClientRequestBuilder) Closure callable) {
        arguments.interceptor = createInterceptor(callable)
        return currentRestInstanceApi().insert((D)this, arguments)
    }

    /**
     * Saves an entity using the given closure to customize the request
     *
     * @param callable The callable
     * @return An observable
     */
    Observable<D> save(Map arguments, @DelegatesTo(HttpClientRequestBuilder) Closure callable) {
        arguments.interceptor = createInterceptor(callable)
        return currentRestInstanceApi().save((D)this, arguments)

    }

    /**
     * Deletes an entity using the given closure to customize the request
     *
     * @param callable The callable
     * @return An observable
     */
    Observable<Boolean> delete(Map arguments, @DelegatesTo(HttpClientRequestBuilder) Closure callable) {
        arguments.interceptor = createInterceptor(callable)
        return currentRestInstanceApi().delete((D)this, arguments)
    }

    /**
     * Construct and object from JSON
     *
     * @param json The JSON string
     * @return The object
     */
    static Observable<D> fromJson(String json) {
        fromJson((Reader)new StringReader(json))
    }

    /**
     * Construct and object from JSON
     *
     * @param reader The reader
     * @return The object
     */
    static Observable<D> fromJson(Reader reader) {
        return (Observable<D>)currentRestStaticApi().fromJson(reader)
    }

    /**
     * Retrieve an instance by id
     *
     * @param id The id of the instance
     * @return An observable
     */
    static Observable<D> get(Serializable id, @DelegatesTo(HttpClientRequestBuilder) Closure callable) {
        (Observable<D>)currentRestStaticApi().get(id, callable)
    }
    /**
     * Retrieve an instance by id
     *
     * @param id The id of the instance
     * @return An observable
     */
    static Observable<D> get(Serializable id, Map arguments, @DelegatesTo(HttpClientRequestBuilder) Closure callable) {
        (Observable<D>) currentRestStaticApi().get(id, arguments, callable)
    }

    /**
     * List all entities and return an observable
     *
     * @return An observable with all results
     */
    static Observable<List<D>> list(@DelegatesTo(HttpClientRequestBuilder) Closure callable) {
        (Observable<List<D>>)currentRestStaticApi().list(Collections.emptyMap(), callable)
    }

    /**
     * Finds all entities and return an observable
     *
     * @return An observable with all results
     */
    static Observable<D> findAll( @DelegatesTo(HttpClientRequestBuilder) Closure callable ) {
        (Observable<D>)currentRestStaticApi().findAll(Collections.emptyMap(), callable)
    }

    /**
     * Finds all entities and return an observable
     *
     * @return An observable with all results
     */
    static Observable<D> findAll(Map args,  @DelegatesTo(HttpClientRequestBuilder) Closure callable) {
        currentRestStaticApi().findAll(args, callable)
    }

    /**
     * List all entities and return an observable
     *
     * @return An observable with all results
     */
    static Observable<List<D>> list(Map args, @DelegatesTo(HttpClientRequestBuilder) Closure callable) {
        return (Observable<List<D>>)currentRestStaticApi().list(args, callable)
    }


    /**
     *
     * @param callable Callable closure containing detached criteria definition
     * @return The DetachedCriteria instance
     */
    static RestDetachedCriteria<D> where(@DelegatesTo(RestDetachedCriteria) Closure callable) {
        currentRestStaticApi().where callable
    }

    /**
     *
     * @param callable Callable closure containing detached criteria definition
     * @return The DetachedCriteria instance that is lazily initialized
     */
    static RestDetachedCriteria<D> whereLazy(@DelegatesTo(RestDetachedCriteria) Closure callable) {
        currentRestStaticApi().whereLazy callable
    }

    /**
     *
     * @param callable Callable closure containing detached criteria definition
     * @return The DetachedCriteria instance
     */
    static RestDetachedCriteria<D> whereAny(@DelegatesTo(RestDetachedCriteria) Closure callable) {
        currentRestStaticApi().whereAny callable
    }

    /**
     * Switches to given named connection within the context of the closure. The delegate of the closure is used to resolve
     * operations against the connection.
     *
     * @param connectionName The name of the connection
     * @param callable The closure
     * @return
     */
    static <T> T withConnection(String connectionName, @DelegatesTo(RxRestGormAllOperations) Closure<T> callable ) {
        def staticOperations = (RxGormAllOperations<D>) RxGormEnhancer.findStaticApi(this, connectionName)
        callable.setDelegate(staticOperations)
        return callable.call()
    }

    private static RequestBuilderInterceptor createInterceptor(Closure callable) {
        return new ExistingClosureRequestBuilderInteceptor(callable)
    }

    private static RxRestGormStaticApi<D> currentRestStaticApi() {
        (RxRestGormStaticApi)RxGormEnhancer.findStaticApi(this)
    }

    private static RxRestGormInstanceApi<D> currentRestInstanceApi() {
        (RxRestGormInstanceApi)RxGormEnhancer.findInstanceApi(this)
    }
}
