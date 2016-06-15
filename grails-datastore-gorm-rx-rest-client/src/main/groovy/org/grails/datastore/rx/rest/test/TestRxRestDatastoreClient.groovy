package org.grails.datastore.rx.rest.test

import groovy.transform.CompileStatic
import org.grails.datastore.rx.rest.RxRestDatastoreClient
import org.grails.datastore.rx.rest.http.netty.HttpRequestBuilder
import org.grails.datastore.rx.rest.http.test.HttpTestServer
import org.grails.datastore.rx.rest.http.test.TestHttpServerRequestBuilder
import org.springframework.core.env.PropertyResolver

/**
 * <p>A Test client that can be used in unit tests to verify requests and stub responses.</p>
 *
 * <p>Example:</p>
 *
 * <pre>
 * <code>
 *    TestRxRestDatastoreClient client = new TestRxRestDatastoreClient(Person)
 *    def mock = client.expect {
 *            uri '/person/1'
 *    }
 *    .respond {
 *        json {
 *            id 1
 *            name "Fred"
 *            age 10
 *            dateOfBirth "2006-07-09T00:00+0000"
 *        }
 *   }
 *
 *   Person p = Person.get(1).toBlocking().first()
 *   mock.verify()
 *  </code>
 * </pre>
 *
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

    /**
     * Reset the state of the mock
     */
    void reset() {
        httpTestServer.reset()
    }

    /**
     * Add expectations
     *
     * @param callable The callable
     * @return A {@link TestHttpServerRequestBuilder}
     */
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
