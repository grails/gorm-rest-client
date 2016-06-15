package org.grails.datastore.rx.rest.http.test

import groovy.transform.CompileStatic
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.DefaultFullHttpRequest
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpVersion
import io.reactivex.netty.protocol.http.server.HttpServerRequest
import org.grails.datastore.rx.rest.http.server.HttpServerRequestBuilder
import org.grails.datastore.rx.rest.http.server.HttpServerResponseBuilder
import rx.Observable

import java.nio.charset.Charset

/**
 * Created by graemerocher on 15/06/16.
 */
@CompileStatic
class TestHttpServerRequestBuilder {
    List<HttpServerRequest> expectedRequests = []
    List<HttpServerRequest> inboundMessages = []
    int expectedTotal = 0
    Charset charset = Charset.forName("UTF-8")
    Closure responseClosure

    TestHttpServerRequestBuilder expect(@DelegatesTo(HttpServerRequestBuilder) Closure callable) {
        expectedTotal++
        def req = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/")
        def builder = new HttpServerRequestBuilder(req)
        callable.delegate = builder
        callable.call()
        expectedRequests.add(builder.clientRequest)
        return this
    }

    void reset() {
        expectedRequests.clear()
        inboundMessages.clear()
        expectedTotal = 0
        responseClosure = null
    }

    TestHttpServerRequestBuilder respond(@DelegatesTo(HttpServerResponseBuilder) Closure callable) {
        this.responseClosure = callable
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

                HttpServerRequest expectedRequest = expectedRequests.get(i++)
                HttpServerRequest actualRequest = (HttpServerRequest)object

                verifyRequest(expectedRequest, actualRequest)
            }
            else {
                assert false : "Found non-request object among outbound messages"
            }
        }
        inboundMessages.clear()
        return true
    }

    protected void verifyRequest(HttpServerRequest expected, HttpServerRequest actual) {
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
                assert "$headerName: $expectedHeaderValue" == "$headerName: $actualHeaderValue"
            }
        }

//      // TODO: Fix reading body
//            def emptyBody = Observable.just(Unpooled.copiedBuffer("", charset))
//            ByteBuf expectedBody = (ByteBuf)expected.content.switchIfEmpty(emptyBody).toBlocking().first()
//            def actualBody = (ByteBuf)actual.content.switchIfEmpty(emptyBody).toBlocking().first()
//            if( expectedBody.hasArray() && !actualBody.hasArray() ) {
//                assert false : "Expected content ${expectedBody.toString()} but got none"
//            }
//            else {
//                assert expectedBody.toString(charset) == actualBody.toString(charset)
//            }

    }
}
