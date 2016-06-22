package org.grails.datastore.rx.rest.api

import grails.gorm.rx.DetachedCriteria
import grails.gorm.rx.rest.RestDetachedCriteria
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
}
