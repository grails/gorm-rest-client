package org.grails.datastore.rx.rest.query

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufHolder
import io.netty.buffer.ByteBufInputStream
import io.netty.buffer.Unpooled
import io.netty.handler.codec.base64.Base64
import io.netty.handler.codec.http.HttpHeaderNames
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
import org.grails.datastore.mapping.query.QueryException
import org.grails.datastore.rx.query.QueryState
import org.grails.datastore.rx.query.RxQuery
import org.grails.datastore.rx.rest.RxRestDatastoreClient
import rx.Observable
import rx.Observer
import rx.Subscriber
import rx.observables.AsyncOnSubscribe
import rx.subjects.PublishSubject

/**
 * An implementation of {@link RxQuery} for REST that only supports the {@link org.grails.datastore.mapping.query.Query.Equals} constraint, converting each one into a request parameter
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
@Slf4j
class SimpleRxRestQuery<T> extends Query implements RxQuery<T> {

    protected static final char AMPERSAND = '&'
    protected static final char EQUALS = '='

    final RxRestDatastoreClient datastoreClient
    final QueryState queryState

    protected boolean singleResult = false
    protected Serializable id = null
    protected String uri
    protected Class<T> type

    SimpleRxRestQuery(RxRestDatastoreClient client, PersistentEntity entity, QueryState queryState = new QueryState()) {
        super(null, entity)
        this.datastoreClient = client
        this.queryState = queryState
        this.type = entity.getJavaClass()
        this.uri = datastoreClient.pathResolver.getPath(type)
    }

    @Override
    Observable<T> findAll() {
        HttpClient httpClient = datastoreClient.createHttpClient()

        Codec codec = datastoreClient.getMappingContext().get(type, datastoreClient.codecRegistry)

        StringBuilder queryParameters = buildParameters()

        if(id != null) {
            T loadedEntity = queryState.getLoadedEntity(type, id)
            if(loadedEntity != null) {
                return Observable.just(loadedEntity)
            }
        }

        def queryString = queryParameters.toString()
        if(queryString.length() > 1) {
            uri = "${uri}${queryString}"
        }

        HttpClientRequest httpClientRequest = httpClient.createGet(uri)

        httpClientRequest = datastoreClient.prepareRequest(httpClientRequest)

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

            if(singleResult) {
                return Observable.create( { Subscriber subscriber ->
                    def reader = new InputStreamReader(new ByteBufInputStream(byteBuf))
                    try {
                        def decoded = codec.decode(new JsonReader(reader), BsonPersistentEntityCodec.DEFAULT_DECODER_CONTEXT)
                        subscriber.onNext decoded
                    }
                    catch(Throwable e) {
                        log.error "Error querying [$entity.name] object for URI [$uri]", e
                        subscriber.onError(e)
                    }
                    finally {
                        subscriber.onCompleted()
                        byteBuf.release()
                        reader.close()
                    }

                } as Observable.OnSubscribe)
            }
            else {
                byteBuf.retain()
                long readCount = 0
                return Observable.create(new AsyncOnSubscribe<JsonReader, T>() {

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
                        try {
                            if(readCount < requested && !endOfDocument) {
                                T nextEntity = codec.decode(jsonReader, BsonPersistentEntityCodec.DEFAULT_DECODER_CONTEXT)
                                readCount++
                                observer.onNext(Observable.just(nextEntity))
                            }
                        } catch (Throwable e) {
                            log.error "Error querying [$entity.name] entities for URI [$uri]", e
                            observer.onError(e)
                        }

                        if(endOfDocument) {
                            observer.onCompleted()
                        }

                        return jsonReader
                    }
                })
            }


        }

    }

    protected StringBuilder buildParameters() {
        Query.Junction junction = criteria

        return buildParameters(junction)

    }

    protected StringBuilder buildParameters(Query.Junction junction) {
        if (!(junction instanceof Query.Conjunction)) {
            throw new QueryException("Only conjunctions are supported by this query implementation")
        } else {

            def allCriteria = junction.criteria
            StringBuilder queryParameters = new StringBuilder("?")
            boolean first = true

            if (!allCriteria.isEmpty()) {


                for (Query.Criterion c in allCriteria) {
                    if (c instanceof Query.Conjunction) {
                        return buildParameters((Query.Conjunction)c)
                    } else if (c instanceof Query.IdEquals) {
                        uri = "$uri/${((Query.IdEquals) c).getValue()}"
                        singleResult = true
                    } else if (c instanceof Query.Equals) {
                        Query.Equals equals = (Query.Equals) c
                        def value = equals.value

                        if (equals.property == entity.getIdentity().name) {
                            id = (Serializable) value
                            uri = "$uri/${value}"
                            singleResult = true
                        } else {

                            if (first) {
                                first = false
                            } else {
                                queryParameters.append(AMPERSAND)
                            }

                            queryParameters
                                    .append(equals.property)
                                    .append(EQUALS)
                                    .append(URLEncoder.encode(value.toString(), datastoreClient.charset.toString()))
                        }
                    }
                }
            }



            if (!singleResult) {
                if (offset > 0) {
                    if (first) {
                        first = false
                    } else {
                        queryParameters.append(AMPERSAND)
                    }
                    queryParameters.append(datastoreClient.offsetParameter)
                            .append(EQUALS)
                            .append(offset)
                }
                if (max > -1) {
                    if (first) {
                        first = false
                    } else {
                        queryParameters.append(AMPERSAND)
                    }
                    queryParameters.append(datastoreClient.maxParameter)
                            .append(EQUALS)
                            .append(max)
                }

                for (Query.Order order in orderBy) {
                    if (first) {
                        first = false
                    } else {
                        queryParameters.append(AMPERSAND)
                    }
                    queryParameters.append(datastoreClient.sortParameter)
                            .append(EQUALS)
                            .append(order.property)
                            .append(AMPERSAND)
                            .append(datastoreClient.orderParameter)
                            .append(EQUALS)
                            .append(order.direction.name().toLowerCase())
                }
            }
            return queryParameters
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
