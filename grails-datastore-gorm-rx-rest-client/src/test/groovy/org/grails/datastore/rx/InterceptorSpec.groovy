package org.grails.datastore.rx

import grails.gorm.rx.RxEntity
import grails.gorm.rx.rest.RestDetachedCriteria
import grails.gorm.rx.rest.interceptor.RequestBuilderInterceptor
import grails.http.HttpMethod
import grails.http.MediaType
import io.reactivex.netty.protocol.http.client.HttpClientRequest
import org.grails.datastore.mapping.core.DatastoreUtils
import org.grails.datastore.rx.domain.Intercepted
import org.grails.datastore.rx.rest.RestEndpointPersistentEntity
import org.grails.datastore.rx.rest.config.Settings
import org.grails.datastore.rx.rest.test.TestRxRestDatastoreClient
import rx.Observable

/**
 * Created by graemerocher on 21/06/16.
 */
class InterceptorSpec extends RxGormSpec {
    @Override
    List<Class> getDomainClasses() {
        return [Intercepted]
    }

    void "Test interceptors are resolved and executed correctly for detached criteria"() {
        given:"A canned response"
        def mock = client.expect {
            uri '/intercepted?name=Fred'
            accept(MediaType.JSON)
            header("Foo", "Bar")
            header("One", "Two")
            header("Three", "Four")
        }
        .respond {
            json {
                id 1
                name "Fred"
            }
        }

        when:"A get request is issued"
        RestDetachedCriteria<Intercepted> criteria = Intercepted.where {
            name == "Fred"
        }
        Intercepted p = criteria.get {
            header "Three", "Four"
        }.toBlocking().first()

        then:"The result is correct"
        mock.verify()
        p.name == "Fred"
    }

    void "Test interceptors are resolved and executed correctly for static methods"() {
        given:"A canned response"
        def mock = client.expect {
            uri '/intercepted/1'
            accept(MediaType.JSON)
            header("Foo", "Bar")
            header("One", "Two")
            header("Three", "Four")
        }
        .respond {
            json {
                id 1
                name "Fred"
            }
        }

        when:"A get request is issued"
        Observable<Intercepted> observable = Intercepted.get(1) {
            header "Three", "Four"
        }
        Intercepted p = observable.toBlocking().first()

        then:"The result is correct"
        mock.verify()
        p.name == "Fred"
    }

    void "Test interceptors are resolved and executed correctly for instance methods"() {
        given:"A canned response"
        def mock = client.expect {
            uri '/intercepted'
            method(HttpMethod.POST)
            contentType(MediaType.JSON)
            header("Foo", "Bar")
            header("One", "Two")
            header("Three", "Four")
            json {
                name "Fred"
            }
        }
        .respond {
            created()
            json {
                id 1
                name "Fred"
            }
        }

        when:"A get request is issued"
        Observable<Intercepted> observable = new Intercepted(name: "Fred").save {
            header "Three", "Four"
        }

        Intercepted p = observable.toBlocking().first()

        then:"The result is correct"
        mock.verify()
        p.name == "Fred"
    }

    @Override
    protected TestRxRestDatastoreClient createRestDatastoreClient(List<Class> classes) {
        def config = [(Settings.SETTING_INTERCEPTORS):'org.grails.datastore.rx.TestInterceptor']
        new TestRxRestDatastoreClient(DatastoreUtils.createPropertyResolver(config), classes as Class[])
    }
}

class TestInterceptor extends RequestBuilderInterceptor {
    @Override
    Closure build(RestEndpointPersistentEntity entity, RxEntity instance, HttpClientRequest request) {
        buildRequest {
            header("Foo", "Bar")
        }
    }
}
