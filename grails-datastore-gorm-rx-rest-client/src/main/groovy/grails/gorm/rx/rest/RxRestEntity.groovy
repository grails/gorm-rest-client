package grails.gorm.rx.rest

import grails.gorm.rx.DetachedCriteria
import grails.gorm.rx.RxEntity
import grails.gorm.rx.rest.interceptor.ExistingClosureRequestBuilderInteceptor
import grails.gorm.rx.rest.interceptor.RequestBuilderInterceptor
import grails.http.HttpMethod
import grails.http.client.builder.HttpClientRequestBuilder
import groovy.transform.CompileStatic
import io.reactivex.netty.protocol.http.client.HttpClientRequest
import org.bson.codecs.Codec
import org.grails.datastore.bson.json.JsonReader
import org.grails.datastore.bson.json.JsonWriter
import org.grails.datastore.gorm.schemaless.DynamicAttributes
import org.grails.datastore.rx.bson.codecs.RxBsonPersistentEntityCodec
import org.grails.datastore.rx.rest.RestEndpointPersistentEntity
import org.grails.datastore.rx.rest.RxRestDatastoreClient
import org.grails.datastore.rx.rest.api.RxRestGormStaticApi
import org.grails.datastore.rx.rest.config.Settings
import org.grails.gorm.rx.api.RxGormEnhancer
import org.grails.gorm.rx.api.RxGormStaticApi
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
        return RxEntity.super.save(interceptor: createInterceptor(callable))
    }

    /**
     * Save an entity using the given closure to customize the request
     *
     * @param callable The callable
     * @return An observable
     */
    Observable<D> patch(@DelegatesTo(HttpClientRequestBuilder) Closure callable = null) {
        return patch([:], callable)
    }

    /**
     * Save an entity using the given closure to customize the request
     *
     * @param callable The callable
     * @return An observable
     */
    Observable<D> patch(Map arguments, @DelegatesTo(HttpClientRequestBuilder) Closure callable = null) {
        arguments = arguments == null ? [:] : arguments
        arguments.put(Settings.ARGUMENT_METHOD, HttpMethod.PATCH)
        if(callable != null) {
            arguments.put(Settings.ARGUMENT_INTERCEPTOR, createInterceptor(callable))
        }
        return RxEntity.super.save(arguments)
    }

    /**
     * Save an entity using the given closure to customize the request
     *
     * @param callable The callable
     * @return An observable
     */
    Observable<D> post(@DelegatesTo(HttpClientRequestBuilder) Closure callable = null) {
        return patch([:], callable)
    }

    /**
     * Save an entity using the given closure to customize the request
     *
     * @param callable The callable
     * @return An observable
     */
    Observable<D> post(Map arguments, @DelegatesTo(HttpClientRequestBuilder) Closure callable = null) {
        arguments = arguments == null ? [:] : arguments
        arguments.put(Settings.ARGUMENT_METHOD, HttpMethod.POST)
        if(callable != null) {
            arguments.put(Settings.ARGUMENT_INTERCEPTOR, createInterceptor(callable))
        }
        return RxEntity.super.save(arguments)
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
    Observable<D> insert(Map arguments, @DelegatesTo(HttpClientRequestBuilder) Closure callable) {
        arguments.interceptor = createInterceptor(callable)
        return RxEntity.super.insert(arguments)
    }

    /**
     * Saves an entity using the given closure to customize the request
     *
     * @param callable The callable
     * @return An observable
     */
    Observable<D> save(Map arguments, @DelegatesTo(HttpClientRequestBuilder) Closure callable) {
        arguments.interceptor = createInterceptor(callable)
        return RxEntity.super.save(arguments)

    }

    /**
     * Deletes an entity using the given closure to customize the request
     *
     * @param callable The callable
     * @return An observable
     */
    Observable<Boolean> delete(Map arguments, @DelegatesTo(HttpClientRequestBuilder) Closure callable) {
        arguments.interceptor = createInterceptor(callable)
        return RxEntity.super.delete(arguments)
    }

    /**
     * Construct and object from JSON
     *
     * @param json The JSON string
     * @return The object
     */
    static D fromJson(String json) {
        fromJson((Reader)new StringReader(json))
    }

    /**
     * Construct and object from JSON
     *
     * @param reader The reader
     * @return The object
     */
    static D fromJson(Reader reader) {
        RxRestGormStaticApi restStaticApi = currentRestStaticApi()

        RxRestDatastoreClient client = (RxRestDatastoreClient) restStaticApi.datastoreClient
        Codec<D> codec = (Codec<D>)client.codecRegistry.get(getClass())

        return codec.decode(new JsonReader(reader), RxBsonPersistentEntityCodec.DEFAULT_DECODER_CONTEXT)
    }

    /**
     * Retrieve an instance by id
     *
     * @param id The id of the instance
     * @return An observable
     */
    static Observable<D> get(Serializable id, @DelegatesTo(HttpClientRequestBuilder) Closure callable) {
        (Observable<D>)currentRestStaticApi().get(id, [interceptor:createInterceptor(callable)])
    }
    /**
     * Retrieve an instance by id
     *
     * @param id The id of the instance
     * @return An observable
     */
    static Observable<D> get(Serializable id, Map arguments, @DelegatesTo(HttpClientRequestBuilder) Closure callable) {
        arguments.interceptor = createInterceptor(callable)
        (Observable<D>) currentRestStaticApi().get(id, arguments)
    }

    /**
     * List all entities and return an observable
     *
     * @return An observable with all results
     */
    static Observable<List<D>> list(@DelegatesTo(HttpClientRequestBuilder) Closure callable) {
        (Observable<List<D>>)currentRestStaticApi().list(interceptor:createInterceptor(callable))
    }

    /**
     * Finds all entities and return an observable
     *
     * @return An observable with all results
     */
    static Observable<D> findAll( @DelegatesTo(HttpClientRequestBuilder) Closure callable ) {
        (Observable<D>)currentRestStaticApi().findAll(interceptor:createInterceptor(callable))
    }

    /**
     * Finds all entities and return an observable
     *
     * @return An observable with all results
     */
    static Observable<D> findAll(Map args,  @DelegatesTo(HttpClientRequestBuilder) Closure callable) {
        args.interceptor = createInterceptor(callable)
        currentRestStaticApi().findAll(args)
    }

    /**
     * List all entities and return an observable
     *
     * @return An observable with all results
     */
    static Observable<List<D>> list(Map args, @DelegatesTo(HttpClientRequestBuilder) Closure callable) {
        args.interceptor = createInterceptor(callable)
        (Observable<List<D>>)currentRestStaticApi().list(args)
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

    private static RequestBuilderInterceptor createInterceptor(Closure callable) {
        return new ExistingClosureRequestBuilderInteceptor(callable)
    }

    private static RxRestGormStaticApi<RxRestEntity> currentRestStaticApi() {
        (RxRestGormStaticApi)RxGormEnhancer.findStaticApi(this)
    }
}
