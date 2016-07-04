package org.grails.datastore.rx.rest.test

import grails.http.client.test.HttpTestServer
import groovy.transform.CompileStatic
import io.reactivex.netty.client.ConnectionProviderFactory
import org.grails.datastore.mapping.core.connections.DefaultConnectionSource
import org.grails.datastore.rx.rest.connections.RestConnectionSourceSettings

/**
 * A test connection source that constructs an {@link HttpTestServer}
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@CompileStatic
class TestConnectionSource extends DefaultConnectionSource<ConnectionProviderFactory, RestConnectionSourceSettings> {

    final HttpTestServer httpTestServer

    TestConnectionSource(String name, ConnectionProviderFactory source, RestConnectionSourceSettings settings) {
        super(name, source, settings)

        this.httpTestServer = initializeTestClient()
        def address = (InetSocketAddress) httpTestServer.socketAddress
        settings.host( "http://localhost:$address.port" )
    }

    protected HttpTestServer initializeTestClient() {
        HttpTestServer httpTestServer = new HttpTestServer()
        return httpTestServer
    }


    @Override
    public void close() throws IOException {
        super.close()
        httpTestServer.close()
    }

}
