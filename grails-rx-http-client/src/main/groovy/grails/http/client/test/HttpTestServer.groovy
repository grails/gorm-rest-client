package grails.http.client.test

import grails.http.client.builder.server.HttpServerResponseBuilder
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.reactivex.netty.protocol.http.server.HttpServer
import io.reactivex.netty.protocol.http.server.HttpServerRequest
import io.reactivex.netty.protocol.http.server.HttpServerResponse
import io.reactivex.netty.protocol.http.server.RequestHandler
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

    @Delegate TestHttpServerRequestBuilder requestBuilder
    final HttpServer server
    final SocketAddress socketAddress

    HttpTestServer() {
        server = HttpServer.newServer()
        socketAddress = startServer()
        requestBuilder = new TestHttpServerRequestBuilder(this)
    }

    HttpTestServer(String address) {
        URI uri = new URI(address)
        server = HttpServer.newServer(new InetSocketAddress(uri.host, uri.port))
        socketAddress = startServer()
        requestBuilder = new TestHttpServerRequestBuilder(this)
    }

    public SocketAddress startServer() {
        int requestCount = 0
        server.start(new RequestHandler() {
            @Override
            Observable<Void> handle(HttpServerRequest request, HttpServerResponse response) {
                requestBuilder.inboundMessages.add(request)
                HttpServerResponseBuilder builder = new HttpServerResponseBuilder(response, Charset.forName("UTF-8"))

                Closure responseClosure = requestCount < requestBuilder.expectedResponses.size() ? requestBuilder.expectedResponses[requestCount++] : null
                if(responseClosure != null) {
                    responseClosure.setDelegate(builder)
                    try {
                        responseClosure.call()
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
