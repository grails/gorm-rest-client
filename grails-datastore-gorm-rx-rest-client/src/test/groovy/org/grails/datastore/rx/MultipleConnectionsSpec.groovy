package org.grails.datastore.rx

import grails.gorm.annotation.Entity
import grails.gorm.rx.rest.RxRestEntity
import grails.gorm.rx.rest.interceptor.InterceptorContext
import grails.gorm.rx.rest.interceptor.RequestBuilderInterceptor
import io.reactivex.netty.protocol.http.client.HttpClientRequest
import org.grails.datastore.mapping.core.DatastoreUtils
import org.grails.datastore.mapping.core.connections.ConnectionSource
import org.grails.datastore.rx.domain.Person
import org.grails.datastore.rx.rest.config.Settings
import org.grails.datastore.rx.rest.test.TestRxRestDatastoreClient

/**
 * Created by graemerocher on 04/07/16.
 */
class MultipleConnectionsSpec extends RxGormSpec {
    @Override
    List<Class> getDomainClasses() {
        [CompanyA]
    }

    void "Test query multiple data sources"() {
        given:"A canned response"
        def mock = client.expect {
            uri '/companyA/1'
            method "DELETE"
        }.respond {
            noContent()
        }

        def mock2 = client.getDatastoreClient("test2").expect {
            uri '/companyA/1'
            method "DELETE"
            header("Second-Server", "True")
        }.respond {
            noContent()
        }

        when:"A get request is issued"

        CompanyA p = new CompanyA(name: "Test")
        p.id = 1L
        boolean deleted = p.delete().toBlocking().first()
        boolean deleted2 = CompanyA.withConnection("test2") {
            delete(p).toBlocking().first()
        }

        then:"The result is correct"
        mock.verify()
        mock2.verify()
        deleted
        deleted2
    }
    @Override
    protected TestRxRestDatastoreClient createRestDatastoreClient(List<Class> classes) {
        Map config = [
                (Settings.SETTING_HOST)       : "http://localhost/",
                (Settings.SETTING_CONNECTIONS): [
                        test1: [
                                url: "http://localhost/"
                        ],
                        test2: [
                                url: "http://localhost/",
                                interceptors:SecondConnectionInterceptor
                        ]
                ]
        ]
        return new TestRxRestDatastoreClient([ConnectionSource.DEFAULT, "test1", "test2"], DatastoreUtils.createPropertyResolver(config), CompanyA)
    }
}

@Entity
class CompanyA implements RxRestEntity<CompanyA> {
    String name

    static mapping = {
        connections ConnectionSource.DEFAULT, "test1", "test2"
    }

}

class SecondConnectionInterceptor extends RequestBuilderInterceptor {

    @Override
    Closure build(HttpClientRequest request, InterceptorContext context) {
        buildRequest {
            header("Second-Server", "True")
        }
    }
}
