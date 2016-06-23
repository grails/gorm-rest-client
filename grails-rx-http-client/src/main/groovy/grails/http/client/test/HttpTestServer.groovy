package grails.http.client.test

import grails.http.client.builder.server.HttpServerResponseBuilder
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufHolder
import io.netty.handler.codec.http.FullHttpRequest
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
                def expectedResponses = requestBuilder.expectedResponses
                FullHttpRequest fullHttpRequest = requestBuilder.expectedFullRequests[requestCount]
                List inboundBodies = requestBuilder.inboundBodies

                if(fullHttpRequest.content().array().length > 0) {

                    return request.content.switchMap { Object content ->
                        if(content instanceof ByteBuf) {
                            inboundBodies.add((ByteBuf)content)
                        }
                        else if(content instanceof ByteBufHolder) {
                            inboundBodies.add(((ByteBufHolder)content).content())
                        }

                        HttpServerResponseBuilder builder = new HttpServerResponseBuilder(response, Charset.forName("UTF-8"))
                        Closure responseClosure = requestCount < expectedResponses.size() ? expectedResponses[requestCount++] : null
                        if(responseClosure != null) {
                            responseClosure.setDelegate(builder)
                            try {
                                responseClosure.call()
                            } catch (Throwable e) {
                                log.error("respond block produced error: $e.message", e )
                                throw e
                            }
                        }

                        return Observable.empty()
                    }
                }
                else {
                    HttpServerResponseBuilder builder = new HttpServerResponseBuilder(response, Charset.forName("UTF-8"))
                    Closure responseClosure = requestCount < expectedResponses.size() ? expectedResponses[requestCount++] : null
                    if(responseClosure != null) {
                        responseClosure.setDelegate(builder)
                        try {
                            responseClosure.call()
                        } catch (Throwable e) {
                            log.error("respond block produced error: $e.message", e )
                            throw e
                        }
                    }

                    return Observable.empty()
                }
            }
        });
        return server.getServerAddress();
    }

    @Override
    void close() throws IOException {
        server.shutdown()
    }
}
