package grails.http.client.test

import grails.http.client.builder.server.HttpServerRequestBuilder
import grails.http.client.builder.server.HttpServerResponseBuilder
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import io.netty.buffer.ByteBuf
import io.netty.handler.codec.http.DefaultFullHttpRequest
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpVersion
import io.reactivex.netty.protocol.http.server.HttpServerRequest

import java.nio.charset.Charset
/**
 * Allows for mocking and testing usages of {@link grails.http.client.RxHttpClientBuilder} in application code
 *
 * <p>Below is an example:
 * <pre class="code">
 *   client.expect {
 *       uriTemplate '/foo/bar'
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
 *    Observable<Person> p = client.post("https://localhost:8080/foo/bar") {
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
class TestHttpServerRequestBuilder {
    List<HttpServerRequest> expectedRequests = []
    List<FullHttpRequest> expectedFullRequests = []
    List<Closure> expectedResponses = []
    List<HttpServerRequest> inboundMessages = []
    List<ByteBuf> inboundBodies = []

    int expectedTotal = 0
    Charset charset = Charset.forName("UTF-8")
    HttpTestServer httpTestServer

    TestHttpServerRequestBuilder(HttpTestServer httpTestServer) {
        this.httpTestServer = httpTestServer
    }

    InetSocketAddress socketAddress() {
        (InetSocketAddress)this.httpTestServer.socketAddress
    }

    URI serverURI() {
        def address = socketAddress()
        return new URI("http://localhost:${address.port}")
    }

    TestHttpServerRequestBuilder expect(@DelegatesTo(HttpServerRequestBuilder) Closure callable) {
        expectedTotal++
        def req = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/")
        def builder = new HttpServerRequestBuilder(req)
        callable.delegate = builder
        callable.call()
        expectedFullRequests.add(req)
        expectedRequests.add(builder.clientRequest)
        return this
    }

    void reset() {
        expectedRequests.clear()
        inboundMessages.clear()
        expectedTotal = 0
        expectedResponses.clear()
    }

    TestHttpServerRequestBuilder respond(@DelegatesTo(HttpServerResponseBuilder) Closure callable) {
        expectedResponses.add(callable)
        return this
    }

    /**
     * Verifies the expected requests were produced
     */
    boolean verify() {

        def actualTotal = inboundMessages.size()
        if(actualTotal != expectedTotal) {
            assert expectedTotal == actualTotal : "Expected $expectedTotal requests, but $actualTotal were executed"
        }
        int i = 0
        for(object in inboundMessages) {
            if(object instanceof HttpServerRequest) {

                HttpServerRequest expectedRequest = expectedRequests.get(i)
                HttpServerRequest actualRequest = (HttpServerRequest)object

                verifyRequest(expectedRequest, actualRequest, i )
                i++
            }
            else {
                assert false : "Found non-request object among outbound messages"
            }
        }
        expectedRequests.clear()
        expectedTotal = 0
        inboundMessages.clear()
        return true
    }

    protected void verifyRequest(HttpServerRequest expected, HttpServerRequest actual, int index) {
        def expectedUri = expected.uri
        def actualUri = actual.uri

        assert expectedUri == actualUri: "Expected URI [$expectedUri] does not match actual URI [$actualUri]"

        def expectedMethod = expected.httpMethod
        def actualMethod = actual.httpMethod

        assert expectedMethod == actualMethod: "Expected method [$expectedMethod] does not match actual method [$actualMethod]"
        def expectedHeaders = expected.headerNames
        for (headerName in expectedHeaders) {
            def expectedHeaderValue = expected.getHeader(headerName)
            def actualHeaderValue = actual.getHeader(headerName)
            if(expectedHeaderValue.startsWith('multipart/')) {
                // need to ignore the changeable boundary definition
                assert actualHeaderValue.startsWith("multipart/") : "expected a multipart request"
            }
            else {
                String expectedHeader = "$headerName: $expectedHeaderValue"
                String actualHeader = "$headerName: $actualHeaderValue"
                assert expectedHeader == actualHeader
            }
        }

        ByteBuf expectedBody = (ByteBuf) getExpectedBody(expected)
        ByteBuf actualBody = inboundBodies.size() > index ? inboundBodies[index] : null

        if(expectedBody == null && actualBody == null) {
            assert true
        }
        else if(expectedBody == null && actualBody != null) {
            assert false : "Expected no content but got ${actualBody.toString(charset)}"
        }
        else if(expectedBody != null && actualBody == null) {
            assert false : "Expected content ${expectedBody.toString(charset)} but got none"
        }
        else {
            assert expectedBody.toString(charset) == actualBody.toString(charset)
        }

    }

    @CompileDynamic
    protected ByteBuf getExpectedBody(HttpServerRequest expected) {
        ByteBuf byteBuf = ((FullHttpRequest) expected.@nettyRequest).content()
        if(byteBuf != null && byteBuf.array().length > 0) {
            return byteBuf
        }
        return null
    }
}
