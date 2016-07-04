package org.grails.datastore.rx.rest.api

import grails.gorm.rx.rest.RestDetachedCriteria
import grails.gorm.rx.rest.api.RxRestGormAllOperations
import grails.gorm.rx.rest.interceptor.ExistingClosureRequestBuilderInteceptor
import grails.gorm.rx.rest.interceptor.RequestBuilderInterceptor
import grails.http.client.builder.HttpClientRequestBuilder
import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import org.bson.codecs.Codec
import org.grails.datastore.bson.json.JsonReader
import org.grails.datastore.rx.bson.codecs.RxBsonPersistentEntityCodec
import org.grails.datastore.rx.rest.RxRestDatastoreClient
import org.grails.gorm.rx.api.RxGormEnhancer
import org.grails.gorm.rx.api.RxGormStaticApi
import rx.Observable
import rx.Subscriber

/**
 *
 * Static API implementation for RxGORM for REST
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
@InheritConstructors
class RxRestGormStaticApi<D> extends RxGormStaticApi<D> implements RxRestGormAllOperations<D> {

    @Override
    RestDetachedCriteria<D> where(Closure callable) {
        new RestDetachedCriteria<D>(persistentClass).build(callable)
    }

    @Override
    RestDetachedCriteria<D> whereLazy(Closure callable) {
        new RestDetachedCriteria<D>(persistentClass).buildLazy(callable)
    }

    @Override
    RestDetachedCriteria<D> whereAny(Closure callable) {
        (RestDetachedCriteria<D>)new RestDetachedCriteria<D>(persistentClass).or(callable)
    }

    @Override
    Observable<D> fromJson(String json) {
        return fromJson(new StringReader(json))
    }

    Observable<D> fromJson(Reader reader) {
        Observable.create( { Subscriber subscriber ->
            RxRestDatastoreClient client = (RxRestDatastoreClient) datastoreClient
            Codec<D> codec = (Codec<D>)client.codecRegistry.get(getClass())

            try {
                subscriber.onNext codec.decode(new JsonReader(reader), RxBsonPersistentEntityCodec.DEFAULT_DECODER_CONTEXT)
            } catch (Throwable e) {
                subscriber.onError(e)
            }
            finally {
                subscriber.onCompleted()
                reader.close()
            }

        } as Observable.OnSubscribe<D>)
    }

    @Override
    Observable<D> get(Serializable id, @DelegatesTo(HttpClientRequestBuilder) Closure callable) {
        super.get(id, [interceptor:createInterceptor(callable)])
    }

    @Override
    Observable<D> get(Serializable id, Map arguments, @DelegatesTo(HttpClientRequestBuilder) Closure callable) {
        arguments.interceptor = createInterceptor(callable)
        (Observable<D>) super.get(id, arguments)
    }

    @Override
    Observable<List<D>> list(@DelegatesTo(HttpClientRequestBuilder) Closure callable) {
        super.list(interceptor:createInterceptor(callable))
    }

    @Override
    Observable<D> findAll(Map args, @DelegatesTo(HttpClientRequestBuilder) Closure callable) {
        super.findAll(interceptor:createInterceptor(callable))
    }

    @Override
    Observable<List<D>> list(Map args, @DelegatesTo(HttpClientRequestBuilder) Closure callable) {
        args.interceptor = createInterceptor(callable)
        super.list(args)
    }

    private static RequestBuilderInterceptor createInterceptor(Closure callable) {
        if(callable == null) {
            return null
        }
        return new ExistingClosureRequestBuilderInteceptor(callable)
    }


    @Override
    String toJson(D instance) {
        return findCurrentInstanceApi().toJson(instance)
    }

    @Override
    void toJson(D instance, Writer writer) {
        findCurrentInstanceApi().toJson(instance, writer)
    }

    @Override
    Observable<D> save(D instance, @DelegatesTo(HttpClientRequestBuilder) Closure callable) {
        return findCurrentInstanceApi().save(instance, callable)
    }

    @Override
    Observable<D> patch(D instance, @DelegatesTo(HttpClientRequestBuilder) Closure callable) {
        return findCurrentInstanceApi().patch(instance, callable)
    }

    @Override
    Observable<D> patch(D instance, Map arguments = [:], @DelegatesTo(HttpClientRequestBuilder) Closure callable = null) {
        return findCurrentInstanceApi().patch(instance, arguments, callable)
    }


    @Override
    Observable<D> post(D instance, @DelegatesTo(HttpClientRequestBuilder) Closure callable) {
        return post(instance, [:], callable)
    }

    @Override
    Observable<D> post(D instance, Map arguments = [:], @DelegatesTo(HttpClientRequestBuilder) Closure callable = null) {
        return findCurrentInstanceApi().patch(instance, arguments, callable)
    }


    @Override
    Observable<Boolean> delete(D instance, @DelegatesTo(HttpClientRequestBuilder) Closure callable) {
        return findCurrentInstanceApi().delete(instance, callable)
    }

    @Override
    Observable<D> insert(D instance, @DelegatesTo(HttpClientRequestBuilder) Closure callable) {
        return insert(instance, [:], callable)
    }

    @Override
    Observable<D> insert(D instance, Map arguments = [:], @DelegatesTo(HttpClientRequestBuilder) Closure callable = null) {
        return findCurrentInstanceApi().insert(instance, arguments, callable)
    }

    @Override
    Observable<D> save(D instance, Map arguments = [:], @DelegatesTo(HttpClientRequestBuilder) Closure callable = null) {
        return findCurrentInstanceApi().save(instance, arguments, callable)
    }

    @Override
    Observable<Boolean> delete(D instance, Map arguments = [:], @DelegatesTo(HttpClientRequestBuilder) Closure callable = null) {
        return findCurrentInstanceApi().delete(instance, arguments, callable)
    }

    protected RxRestGormInstanceApi findCurrentInstanceApi() {
        return (RxRestGormInstanceApi) RxGormEnhancer.findInstanceApi(persistentClass, datastoreClient.connectionSources.defaultConnectionSource.name)
    }
}
