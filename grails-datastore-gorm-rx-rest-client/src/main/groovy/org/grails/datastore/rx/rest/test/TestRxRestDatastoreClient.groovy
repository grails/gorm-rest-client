package org.grails.datastore.rx.rest.test

import groovy.transform.CompileStatic
import org.grails.datastore.rx.rest.RxRestDatastoreClient
import org.grails.datastore.rx.rest.http.netty.HttpRequestBuilder
import org.grails.datastore.rx.rest.http.test.HttpTestServer
import org.grails.datastore.rx.rest.http.test.TestHttpServerRequestBuilder
import org.springframework.core.env.PropertyResolver

/**
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
class TestRxRestDatastoreClient extends RxRestDatastoreClient {

    private static HttpTestServer httpTestServer

    TestRxRestDatastoreClient(PropertyResolver configuration, Class... classes) {
        super(initializeTestClient(), configuration, classes)
    }

    TestRxRestDatastoreClient(Class... classes) {
        super(initializeTestClient(), classes)
    }

    protected static SocketAddress initializeTestClient() {
        httpTestServer = new HttpTestServer()
        return httpTestServer.socketAddress
    }

    HttpTestServer getHttpTestServer() {
        return httpTestServer
    }

    void reset() {
        httpTestServer.reset()
    }

    TestHttpServerRequestBuilder expect(@DelegatesTo(HttpRequestBuilder) Closure callable) {
        httpTestServer.expect callable
    }

    @Override
    void doClose() {
        super.doClose()
        httpTestServer.close()
        httpTestServer = null
    }
}
