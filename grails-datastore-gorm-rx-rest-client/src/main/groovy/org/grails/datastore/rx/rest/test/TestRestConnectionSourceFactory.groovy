package org.grails.datastore.rx.rest.test

import groovy.transform.CompileStatic
import io.reactivex.netty.client.ConnectionProviderFactory
import org.grails.datastore.mapping.core.connections.ConnectionSource
import org.grails.datastore.mapping.core.connections.DefaultConnectionSource
import org.grails.datastore.rx.rest.connections.RestConnectionSourceFactory
import org.grails.datastore.rx.rest.connections.RestConnectionSourceSettings

/**
 * Creates test connections
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@CompileStatic
class TestRestConnectionSourceFactory extends RestConnectionSourceFactory {
    @Override
    protected DefaultConnectionSource<ConnectionProviderFactory, RestConnectionSourceSettings> create(String name, ConnectionProviderFactory connectionProviderFactory, RestConnectionSourceSettings settings) {
        return new TestConnectionSource(name, connectionProviderFactory, settings)
    }
}
