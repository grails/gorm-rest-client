package org.grails.datastore.rx.rest.query

import groovy.transform.CompileStatic
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufHolder
import io.netty.buffer.ByteBufInputStream
import io.netty.util.concurrent.BlockingOperationException
import io.reactivex.netty.protocol.http.client.HttpClient
import io.reactivex.netty.protocol.http.client.HttpClientResponse
import org.grails.datastore.bson.json.JsonReader
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.query.Query
import org.grails.datastore.rx.query.QueryState
import org.grails.datastore.rx.query.RxQuery
import org.grails.datastore.rx.rest.RxRestDatastoreClient
import rx.Observable
import rx.Observer
import rx.observables.AsyncOnSubscribe

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
        String uri = datastoreClient.pathResolver.getPath(entity.getJavaClass())


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

        httpClient.createGet(uri)
                .switchMap { HttpClientResponse response ->
            response.getContent()
        }.map { Object object ->
            ByteBuf byteBuf
            if (object instanceof ByteBuf) {
                byteBuf = (ByteBuf) object
            } else if (object instanceof ByteBufHolder) {
                byteBuf = ((ByteBufHolder) object).content()
            } else {
                throw new IllegalStateException("Received invalid response object: $object")
            }

            def reader = new InputStreamReader(new ByteBufInputStream(byteBuf))
            def jsonReader = new JsonReader(reader)
            try {
                jsonReader.readStartArray()

                jsonReader.readEndArray()
            } finally {
                reader.close()
                byteBuf.release()
            }

        }

        Observable.create(new AsyncOnSubscribe() {
            @Override
            protected Object generateState() {
                return null
            }

            @Override
            protected Object next(Object state, long requested, Observer observer) {
                return null
            }

            @Override
            void call(Object o) {

            }
        })
    }

    @Override
    Observable<T> singleResult() {
        return null
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
