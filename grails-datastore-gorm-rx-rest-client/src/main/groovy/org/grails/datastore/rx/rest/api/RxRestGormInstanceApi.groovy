package org.grails.datastore.rx.rest.api

import grails.gorm.rx.rest.api.RxRestGormInstanceOperations
import grails.gorm.rx.rest.interceptor.ExistingClosureRequestBuilderInteceptor
import grails.gorm.rx.rest.interceptor.RequestBuilderInterceptor
import grails.http.HttpMethod
import grails.http.client.builder.HttpClientRequestBuilder
import groovy.transform.CompileStatic
import org.bson.codecs.Codec
import org.grails.datastore.bson.json.JsonWriter
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.rx.RxDatastoreClient
import org.grails.datastore.rx.bson.codecs.RxBsonPersistentEntityCodec
import org.grails.datastore.rx.rest.RxRestDatastoreClient
import org.grails.datastore.rx.rest.config.Settings
import org.grails.gorm.rx.api.RxGormInstanceApi
import rx.Observable

/**
 * Instance operations for RxGORM for REST
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@CompileStatic
class RxRestGormInstanceApi<D> extends RxGormInstanceApi<D> implements RxRestGormInstanceOperations<D> {
    RxRestGormInstanceApi(PersistentEntity entity, RxDatastoreClient datastoreClient) {
        super(entity, datastoreClient)
    }

    @Override
    String toJson(D instance) {
        def writer = new StringWriter()
        toJson(instance, writer)
        return writer.toString()
    }

    @Override
    void toJson(D instance, Writer writer) {
        RxRestDatastoreClient client = (RxRestDatastoreClient) datastoreClient
        Codec<Object> codec = (Codec<Object>)client.codecRegistry.get(getClass())
        codec.encode(new JsonWriter(writer),(Object)this, RxBsonPersistentEntityCodec.DEFAULT_ENCODER_CONTEXT)
    }

    @Override
    Observable<D> save(D instance, @DelegatesTo(HttpClientRequestBuilder) Closure callable) {
        super.save(instance, [interceptor: createInterceptor(callable)])
    }

    @Override
    Observable<D> patch(D instance, @DelegatesTo(HttpClientRequestBuilder) Closure callable) {
        return patch(instance, [:], callable)
    }


    @Override
    Observable<D> patch(D instance, Map arguments = [:], @DelegatesTo(HttpClientRequestBuilder) Closure callable = null) {
        arguments = populateArgumentsForSave(arguments, HttpMethod.PATCH, callable)
        return super.save(instance, arguments)
    }

    @Override
    Observable<D> post(D instance, @DelegatesTo(HttpClientRequestBuilder) Closure callable) {
        return post(instance, [:], callable)
    }

    @Override
    Observable<D> post(D instance, Map arguments = [:], @DelegatesTo(HttpClientRequestBuilder) Closure callable = null) {
        arguments = populateArgumentsForSave(arguments, HttpMethod.POST, callable)
        return super.save(instance, arguments)
    }


    @Override
    Observable<Boolean> delete(D instance, @DelegatesTo(HttpClientRequestBuilder) Closure callable) {
        return super.delete(instance, [interceptor: createInterceptor(callable)])
    }

    @Override
    Observable<D> insert(D instance, @DelegatesTo(HttpClientRequestBuilder) Closure callable) {
        return super.insert(instance, [interceptor: createInterceptor(callable)])
    }

    @Override
    Observable<D> insert(D instance, Map arguments, @DelegatesTo(HttpClientRequestBuilder) Closure callable) {
        return super.insert(instance, [interceptor: createInterceptor(callable)])
    }

    @Override
    Observable<D> save(D instance, Map arguments, @DelegatesTo(HttpClientRequestBuilder) Closure callable) {
        arguments.interceptor = createInterceptor(callable)
        return super.save(instance, arguments)
    }

    @Override
    Observable<Boolean> delete(D instance, Map arguments, @DelegatesTo(HttpClientRequestBuilder) Closure callable) {
        arguments.interceptor = createInterceptor(callable)
        return super.delete(instance, arguments)
    }

    protected Map populateArgumentsForSave(Map arguments, HttpMethod httpMethod, Closure callable) {
        arguments = arguments == null ? [:] : arguments
        arguments.put(Settings.ARGUMENT_METHOD, httpMethod)
        if (callable != null) {
            arguments.put(Settings.ARGUMENT_INTERCEPTOR, createInterceptor(callable))
        }
        return arguments
    }

    private static RequestBuilderInterceptor createInterceptor(Closure callable) {
        if(callable == null) {
            return null
        }
        return new ExistingClosureRequestBuilderInteceptor(callable)
    }

}
