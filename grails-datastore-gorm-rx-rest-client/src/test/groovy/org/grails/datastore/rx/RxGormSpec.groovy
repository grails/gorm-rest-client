package org.grails.datastore.rx

import groovy.transform.CompileStatic
import org.grails.datastore.bson.json.JsonWriter
import org.grails.datastore.rx.rest.RxRestDatastoreClient
import org.grails.datastore.rx.rest.http.netty.HttpRequestBuilder
import org.grails.datastore.rx.rest.http.test.HttpTestServer
import org.grails.datastore.rx.rest.http.test.TestHttpServerRequestBuilder
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

import java.text.DateFormat
import java.text.SimpleDateFormat

@CompileStatic
abstract class RxGormSpec extends Specification {

    @Shared RxRestDatastoreClient client
    @Shared DateFormat dateFormat
    @Shared @AutoCleanup HttpTestServer httpTestServer

    void setupSpec() {
        dateFormat = new SimpleDateFormat(JsonWriter.ISO_8601)
        TimeZone UTC = TimeZone.getTimeZone("UTC");
        dateFormat.setTimeZone(UTC)

        def classes = getDomainClasses()
        httpTestServer = new HttpTestServer()
        client = new RxRestDatastoreClient(httpTestServer.socketAddress, classes as Class[])
    }

    void cleanupSpec() {
        client?.close()
    }

    void cleanup() {
        httpTestServer.reset()
    }

    TestHttpServerRequestBuilder expect(@DelegatesTo(HttpRequestBuilder) Closure callable) {
        httpTestServer.expect callable
    }

    abstract List<Class> getDomainClasses()
}
