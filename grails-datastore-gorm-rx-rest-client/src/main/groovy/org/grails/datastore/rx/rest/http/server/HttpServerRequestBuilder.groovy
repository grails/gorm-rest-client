package org.grails.datastore.rx.rest.http.server

import groovy.transform.CompileStatic
import io.netty.channel.Channel
import io.netty.channel.embedded.EmbeddedChannel
import io.netty.handler.codec.http.FullHttpRequest
import io.reactivex.netty.protocol.http.server.HttpServerRequest
import io.reactivex.netty.protocol.http.server.HttpServerToConnectionBridge
import io.reactivex.netty.protocol.http.server.events.HttpServerEventPublisher
import org.grails.datastore.rx.rest.http.netty.HttpRequestBuilder

import java.nio.charset.Charset
/**
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
class HttpServerRequestBuilder extends HttpRequestBuilder {

    HttpServerRequestBuilder(FullHttpRequest request, String encoding) {
        this(request, Charset.forName(encoding))
    }

    HttpServerRequestBuilder(FullHttpRequest request, Charset charset = Charset.forName("UTF-8")) {
        super(request, charset)

    }

    HttpServerRequest getClientRequest() {
        return (HttpServerRequest)new StubHttpBridge().newHttpObject(request, new EmbeddedChannel())
    }

    static class StubHttpBridge extends HttpServerToConnectionBridge {
        StubHttpBridge() {
            super(new HttpServerEventPublisher(null))
        }

        @Override
        Object newHttpObject(Object nextItem, Channel channel) {
            return super.newHttpObject(nextItem, channel)
        }
    }
}
