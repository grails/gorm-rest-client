package org.grails.datastore.rx.rest.http.test

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.reactivex.netty.protocol.http.server.HttpServer
import io.reactivex.netty.protocol.http.server.HttpServerRequest
import io.reactivex.netty.protocol.http.server.HttpServerResponse
import io.reactivex.netty.protocol.http.server.RequestHandler
import org.grails.datastore.rx.rest.http.server.HttpServerResponseBuilder
import rx.Observable

import java.nio.charset.Charset

/**
 * A test server to help testing REST endpoints
 *
 * @author Graeme Rocher
 * @since 6.0
 *
 */
@CompileStatic
@Slf4j
class HttpTestServer implements Closeable {

    @Delegate TestHttpServerRequestBuilder requestBuilder = new TestHttpServerRequestBuilder()
    final HttpServer server
    final SocketAddress socketAddress

    HttpTestServer() {
        server = HttpServer.newServer()
        socketAddress = startServer()
    }

    public SocketAddress startServer() {
        server.start(new RequestHandler() {
            @Override
            Observable<Void> handle(HttpServerRequest request, HttpServerResponse response) {
                requestBuilder.inboundMessages.add(request)
                HttpServerResponseBuilder builder = new HttpServerResponseBuilder(response, Charset.forName("UTF-8"))
                if(requestBuilder.responseClosure != null) {
                    requestBuilder.responseClosure.setDelegate(builder)
                    try {
                        requestBuilder.responseClosure.call()
                    } catch (Throwable e) {
                        log.error("respond block produced error: $e.message", e )
                        throw e
                    }
                }
                return builder.response.flushOnlyOnReadComplete()
            }
        });
        return server.getServerAddress();
    }

    @Override
    void close() throws IOException {
        server.shutdown()
    }
}
