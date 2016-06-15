package org.grails.datastore.rx

import groovy.transform.CompileStatic
import io.netty.buffer.ByteBuf
import io.reactivex.netty.protocol.http.client.HttpClient
import io.reactivex.netty.protocol.http.server.HttpServer
import io.reactivex.netty.protocol.http.server.HttpServerRequest
import io.reactivex.netty.protocol.http.server.HttpServerResponse
import io.reactivex.netty.protocol.http.server.RequestHandler
import org.grails.datastore.bson.json.JsonWriter
import org.grails.datastore.rx.rest.RxRestDatastoreClient
import org.grails.datastore.rx.rest.http.server.HttpServerResponseBuilder
import rx.Observable
import spock.lang.Shared
import spock.lang.Specification

import java.nio.charset.Charset
import java.text.DateFormat
import java.text.SimpleDateFormat

@CompileStatic
abstract class RxGormSpec extends Specification {

    @Shared RxRestDatastoreClient client

    @Shared HttpServer server
    @Shared Closure serverResponse = null
    @Shared DateFormat dateFormat

    void setupSpec() {
        dateFormat = new SimpleDateFormat(JsonWriter.ISO_8601)
        TimeZone UTC = TimeZone.getTimeZone("UTC");
        dateFormat.setTimeZone(UTC)

        def classes = getDomainClasses()
        server = HttpServer.newServer()
        SocketAddress serverAddress = startServer()
        client = new RxRestDatastoreClient(serverAddress, classes as Class[])
    }

    void cleanupSpec() {
        client?.close()
        server.shutdown()
    }

    void withResponse(@DelegatesTo(HttpServerResponseBuilder) Closure callable) {
        this.serverResponse = callable
    }

    public SocketAddress startServer() {
        server.start(new RequestHandler() {
            @Override
            Observable<Void> handle(HttpServerRequest request, HttpServerResponse response) {
                HttpServerResponseBuilder builder = new HttpServerResponseBuilder(response, Charset.forName("UTF-8"))
                if(serverResponse != null) {
                    serverResponse.setDelegate(builder)
                    serverResponse.call()
                }
                return builder.response.flushOnlyOnReadComplete()
            }
        });
        return server.getServerAddress();
    }



    abstract List<Class> getDomainClasses()
}
