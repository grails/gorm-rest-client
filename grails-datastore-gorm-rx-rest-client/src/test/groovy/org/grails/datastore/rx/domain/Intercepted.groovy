package org.grails.datastore.rx.domain

import grails.gorm.annotation.Entity
import grails.gorm.rx.RxEntity
import grails.gorm.rx.rest.RxRestEntity
import grails.gorm.rx.rest.interceptor.RequestBuilderInterceptor
import io.reactivex.netty.protocol.http.client.HttpClientRequest
import org.grails.datastore.rx.rest.RestEndpointPersistentEntity

/**
 * Created by graemerocher on 21/06/16.
 */
@Entity
class Intercepted implements RxRestEntity<Intercepted> {

    String name

    static mapping = {
        interceptors MyInterceptor
    }
}

class MyInterceptor extends RequestBuilderInterceptor {

    @Override
    Closure build(RestEndpointPersistentEntity entity, RxEntity instance, HttpClientRequest request) {
        buildRequest {
            header("One", "Two")
        }
    }
}
