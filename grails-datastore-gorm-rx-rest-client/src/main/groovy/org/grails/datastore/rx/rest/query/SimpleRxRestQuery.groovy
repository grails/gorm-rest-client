package org.grails.datastore.rx.rest.query

import groovy.transform.CompileStatic
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufHolder
import io.netty.buffer.ByteBufInputStream
import io.netty.util.concurrent.BlockingOperationException
import io.reactivex.netty.protocol.http.client.HttpClient
import io.reactivex.netty.protocol.http.client.HttpClientRequest
import io.reactivex.netty.protocol.http.client.HttpClientResponse
import org.bson.AbstractBsonReader
import org.bson.BsonType
import org.bson.codecs.Codec
import org.grails.datastore.bson.codecs.BsonPersistentEntityCodec
import org.grails.datastore.bson.json.JsonReader
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.query.Query
import org.grails.datastore.rx.query.QueryState
import org.grails.datastore.rx.query.RxQuery
import org.grails.datastore.rx.rest.RxRestDatastoreClient
import rx.Observable
import rx.Observer
import rx.observables.AsyncOnSubscribe
import rx.subjects.PublishSubject

/**
 * An implementation of {@link RxQuery} for REST that only supports the {@link org.grails.datastore.mapping.query.Query.Equals} constraint, converting each one into a request parameter
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
class SimpleRxRestQuery<T> extends Query implements RxQuery<T> {

    final RxRestDatastoreClient datastoreClient
    final QueryState queryState

    SimpleRxRestQuery(RxRestDatastoreClient client, PersistentEntity entity, QueryState queryState = null) {
        super(null, entity)
        this.datastoreClient = client
        this.queryState = queryState
    }

    @Override
    Observable<T> findAll() {
        HttpClient httpClient = datastoreClient.createHttpClient()
        Class type = entity.getJavaClass()
        String uri = datastoreClient.pathResolver.getPath(type)
        Codec codec = datastoreClient.getMappingContext().get(type, datastoreClient.codecRegistry)

        def allCriteria = criteria.criteria
        if(!allCriteria.isEmpty()) {

            StringBuilder queryParameters = new StringBuilder("?")
            boolean first = true

            for(Query.Criterion c in allCriteria) {
                if(c instanceof Query.IdEquals) {
                    uri = "$uri/${((Query.IdEquals)c).getValue()}"
                }
                else if(c instanceof Query.Equals) {
                    Query.Equals equals = (Query.Equals)c
                    if(first) {
                        first = false
                    }
                    else {
                        queryParameters.append("&")
                    }

                    queryParameters
                            .append(equals.property)
                            .append('=')
                            .append(URLEncoder.encode(equals.value.toString(), datastoreClient.charset.toString()))
                }
            }
            def queryString = queryParameters.toString()
            if(queryString.length() > 1) {
                uri = "${uri}${queryString}"
            }
        }

        HttpClientRequest httpClientRequest = httpClient.createGet(uri)

        httpClientRequest
                .switchMap { HttpClientResponse response ->
            response.getContent()
        }.switchMap { Object object ->
            ByteBuf byteBuf
            if (object instanceof ByteBuf) {
                byteBuf = (ByteBuf) object
            } else if (object instanceof ByteBufHolder) {
                byteBuf = ((ByteBufHolder) object).content()
            } else {
                throw new IllegalStateException("Received invalid response object: $object")
            }

            byteBuf.retain()

            long readCount = 0
            Observable.create(new AsyncOnSubscribe<JsonReader, T>() {

                @Override
                protected JsonReader generateState() {
                    def reader = new InputStreamReader(new ByteBufInputStream(byteBuf))
                    def jsonReader = new JsonReader(reader)
                    jsonReader.readStartArray()
                    return jsonReader
                }

                @Override
                protected void onUnsubscribe(JsonReader reader) {
                    reader.close()
                    byteBuf.release()
                    super.onUnsubscribe(reader)
                }

                @Override
                protected JsonReader next(JsonReader jsonReader, long requested, Observer<Observable<? extends T>> observer) {

                    BsonType bsonType = jsonReader.readBsonType()
                    boolean endOfDocument = bsonType == BsonType.END_OF_DOCUMENT
                    if(readCount < requested && !endOfDocument) {
                        T nextEntity = codec.decode(jsonReader, BsonPersistentEntityCodec.DEFAULT_DECODER_CONTEXT)
                        readCount++
                        observer.onNext(Observable.just(nextEntity))
                    }

                    if(endOfDocument) {
                        observer.onCompleted()
                    }

                    return jsonReader
                }
            })

        }

    }

    @Override
    Observable<T> singleResult() {
        return findAll().first()
    }

    @Override
    protected List executeQuery(PersistentEntity entity, Query.Junction criteria) {
        throw new BlockingOperationException("Cannot perform a blocking operation on a query")
    }

    @Override
    Observable<Number> updateAll(Map properties) {
        throw new UnsupportedOperationException("Batch operations are not supported")
    }

    @Override
    Observable<Number> deleteAll() {
        throw new UnsupportedOperationException("Batch operations are not supported")
    }
}
