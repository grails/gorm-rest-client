package org.grails.datastore.rx.rest.test

import grails.http.client.builder.HttpRequestBuilder
import grails.http.client.test.HttpTestServer
import grails.http.client.test.TestHttpServerRequestBuilder
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import io.reactivex.netty.client.ConnectionProviderFactory
import org.grails.datastore.mapping.core.DatastoreUtils
import org.grails.datastore.mapping.core.connections.*
import org.grails.datastore.rx.RxDatastoreClient
import org.grails.datastore.rx.rest.RxRestDatastoreClient
import org.grails.datastore.rx.rest.config.RestClientMappingContext
import org.grails.datastore.rx.rest.config.Settings
import org.grails.datastore.rx.rest.connections.RestConnectionSourceFactory
import org.grails.datastore.rx.rest.connections.RestConnectionSourceSettings
import org.grails.datastore.rx.rest.connections.RestConnectionSourceSettingsBuilder
import org.springframework.core.env.PropertyResolver

/**
 * <p>A Test client that can be used in unit tests to verify requests and stub responses.</p>
 *
 * <p>Example:</p>
 *
 * <pre>
 * <code>
 *    TestRxRestDatastoreClient client = new TestRxRestDatastoreClient(Person)
 *    def mock = client.expect {
 *            uriTemplate '/person/1'
 *    }
 *    .respond {
 *        json {
 *            id 1
 *            name "Fred"
 *            age 10
 *            dateOfBirth "2006-07-09T00:00+0000"
 *        }
 *   }
 *
 *   Person p = Person.get(1).toBlocking().first()
 *   mock.verify()
 *  </code>
 * </pre>
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
class TestRxRestDatastoreClient extends RxRestDatastoreClient {


    TestRxRestDatastoreClient(PropertyResolver configuration, Class... classes) {
        super(initializeConnectionSources([ConnectionSource.DEFAULT], configuration), classes)
    }

    @CompileDynamic
    TestRxRestDatastoreClient(Class... classes) {
        super(initializeConnectionSources([ConnectionSource.DEFAULT], DatastoreUtils.createPropertyResolver((Settings.SETTING_ALLOW_BLOCKING_OPERATIONS):true)), classes)
    }

    TestRxRestDatastoreClient(Iterable<String> connectionSourceNames, PropertyResolver configuration, Class... classes) {
        super(initializeConnectionSources(connectionSourceNames, configuration), classes)
    }

    @CompileDynamic
    TestRxRestDatastoreClient(Iterable<String> connectionSourceNames, Class... classes) {
        this(connectionSourceNames, DatastoreUtils.createPropertyResolver((Settings.SETTING_ALLOW_BLOCKING_OPERATIONS):true), classes)
    }

    @CompileDynamic
    protected TestRxRestDatastoreClient(ConnectionSources<ConnectionProviderFactory, RestConnectionSourceSettings> connectionSources, Class...classes) {
        super(connectionSources,classes)
    }

    @CompileDynamic
    protected TestRxRestDatastoreClient(ConnectionSources<ConnectionProviderFactory, RestConnectionSourceSettings> connectionSources, RestClientMappingContext mappingContext) {
        super(connectionSources,mappingContext)
    }

    protected static ConnectionSources<ConnectionProviderFactory, RestConnectionSourceSettings> initializeConnectionSources(Iterable<String> connectionSourceNames,  PropertyResolver configuration) {
        def namesList = connectionSourceNames.toList()

        String defaultConnection = namesList.find() { String name -> name == ConnectionSource.DEFAULT } ?: namesList.get(0)

        RestConnectionSourceSettingsBuilder builder = new RestConnectionSourceSettingsBuilder(configuration)
        RestConnectionSourceSettings defaultSettings = builder.build()
        TestRestConnectionSourceFactory factory = new TestRestConnectionSourceFactory()
        TestConnectionSource defaultSource = (TestConnectionSource)factory.create(defaultConnection, defaultSettings)

        List<ConnectionSource<ConnectionProviderFactory, RestConnectionSourceSettings>> otherSources = []
        if(namesList.size() > 1) {
            for(String connectionSourceName in namesList[1..-1]) {

                RestConnectionSourceSettingsBuilder childBuilder = new RestConnectionSourceSettingsBuilder(configuration, SETTING_CONNECTIONS + ".$connectionSourceName", defaultSettings.clone())
                RestConnectionSourceSettings settings = childBuilder.build()
                otherSources.add( factory.create(connectionSourceName, settings) )
            }
        }
        return new StaticConnectionSources(defaultSource, otherSources, configuration)
    }



    HttpTestServer getHttpTestServer() {
        TestConnectionSource testConnectionSource = (TestConnectionSource) getConnectionSources().getDefaultConnectionSource()
        return testConnectionSource.httpTestServer
    }

    @Override
    TestRxRestDatastoreClient getDatastoreClient(String connectionSourceName) {
        return (TestRxRestDatastoreClient)super.getDatastoreClient(connectionSourceName)
    }

    @Override
    protected RxRestDatastoreClient createChildClient(SingletonConnectionSources<ConnectionProviderFactory, RestConnectionSourceSettings> singletonConnectionSources) {
        return new TestRxRestDatastoreClient(singletonConnectionSources, getMappingContext()) {
            @Override
            protected void initialize(RestClientMappingContext mappingContext) {
                // no-op
            }
        }
    }

    /**
     * Reset the state of the mock
     */
    void reset() {
        httpTestServer.reset()
    }

    /**
     * Add expectations
     *
     * @param callable The callable
     * @return A {@link TestHttpServerRequestBuilder}
     */
    TestHttpServerRequestBuilder expect(@DelegatesTo(HttpRequestBuilder) Closure callable) {
        httpTestServer.expect callable
    }

}
