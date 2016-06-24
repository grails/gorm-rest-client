package grails.http.client.test

import grails.http.client.Configuration
import grails.http.client.RxHttpClientBuilder
import grails.http.client.builder.server.HttpServerRequestBuilder
import grails.http.client.cfg.DefaultConfiguration
import groovy.transform.CompileStatic

/**
 * Allows for mocking and testing usages of {@link grails.http.client.RxHttpClientBuilder} in application code
 *
 * <p>Below is an example:
 * <pre class="code">
 *   TestRxHttpClientBuilder client = new TestRxHttpClientBuilder()
 *   client.expect {
 *       uri '/foo/bar'
 *       method "POST"
 *       contentType 'application/json'
 *       json {
 *           title "Ping"
 *       }
 *   }.respond {
 *       created()
 *       json {
 *           title "Pong"
 *       }
 *   }
 *
 *    Observable<HttpClientResponse> p = client.post("https://localhost:8080/foo/bar") {
 *        contentType 'application/json'
 *        json {
 *            title "Ping"
 *        }
 *    }
 *
 *    assert client.verify()
 * </pre>
 *
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@CompileStatic
class TestRxHttpClientBuilder extends RxHttpClientBuilder {

    HttpTestServer currentServer

    TestRxHttpClientBuilder(Configuration configuration = new DefaultConfiguration()) {
        super(configuration)
    }



    /**
     * Adds a new request expectation
     *
     * @param expectedRequest The expected request
     * @return A MockResponseBuilder
     */
    TestHttpServerRequestBuilder expect(String server, @DelegatesTo(HttpServerRequestBuilder) Closure expectedRequest) {
        currentServer?.close()
        currentServer = new HttpTestServer(server)
        currentServer.expect(expectedRequest)
    }

    /**
     * Adds a new request expectation on a ephemeral port
     *
     *
     * @param expectedRequest The expected request
     * @return A MockResponseBuilder
     */
    TestHttpServerRequestBuilder expect(@DelegatesTo(HttpServerRequestBuilder) Closure expectedRequest) {
        currentServer?.close()
        currentServer = new HttpTestServer()
        currentServer.expect(expectedRequest)
    }

    @Override
    void close() throws IOException {
        currentServer?.close()
        super.close()
    }
}
