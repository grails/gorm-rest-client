package org.grails.datastore.rx.rest.api

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import org.grails.gorm.rx.api.RxGormStaticApi
import rx.Observable

/**
 *
 * Static API implementation for RxGORM for REST
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
@InheritConstructors
class RxRestGormStaticApi<D> extends RxGormStaticApi<D> {

    @Override
    Observable<D> get(Serializable id, Map<String, Object> args) {
        return datastoreClient.get(persistentClass, id)
    }
}
