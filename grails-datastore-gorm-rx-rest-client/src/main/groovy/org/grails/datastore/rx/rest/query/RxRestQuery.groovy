package org.grails.datastore.rx.rest.query

import groovy.transform.CompileStatic
import org.grails.datastore.rx.query.QueryState
import org.grails.datastore.rx.query.RxQuery
import org.grails.datastore.rx.rest.RxRestDatastoreClient
import rx.Observable

/**
 *
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
class RxRestQuery<T> implements RxQuery<T> {

    final RxRestDatastoreClient datastoreClient
    final QueryState queryState

    @Override
    Observable<T> findAll() {
        return null
    }

    @Override
    Observable<T> singleResult() {
        return null
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
