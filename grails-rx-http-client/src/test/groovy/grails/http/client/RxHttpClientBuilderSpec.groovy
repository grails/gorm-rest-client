package grails.http.client

import grails.http.HttpMethod
import grails.http.HttpStatus
import grails.http.client.test.TestRxHttpClientBuilder
import groovy.transform.NotYetImplemented
import rx.Observable
import spock.lang.Specification
/**
 * Created by graemerocher on 18/03/16.
 */
class RxHttpClientBuilderSpec extends Specification {

    @NotYetImplemented
    void "Test multipart form submission"() {
        given:"A client"
        RxHttpClientBuilder client = new TestRxHttpClientBuilder()

        def mock = client.expect("http://localhost:8080") {
            uri('/foo/bar')
            method(HttpMethod.POST)
            multipart {
                foo = 'bar'
                file('myFile', 'test.txt', 'Hello world!')
            }
        }.respond {
            ok()
        }

        when:"A form is submitted"
        Observable<HttpClientResponse> p = client.post("http://localhost:8080/foo/bar") {
            multipart {
                foo = "bar"
            }
        }

        def response = p.toBlocking().first()

        then:"The form was submitted correct"
        mock.verify()
        response.status == HttpStatus.OK
    }


    @NotYetImplemented
    void "Test form submission"() {
        given:"A client"
        RxHttpClientBuilder client = new TestRxHttpClientBuilder()

        def mock = client.expect {
            uri('/foo/bar')
            method(HttpMethod.POST)
            contentType('application/x-www-form-urlencoded')
            form {
                foo = 'bar'
            }
        }.respond {
            ok()
        }

        when:"A form is submitted"
        Observable<HttpClientResponse> p = client.post("${mock.serverURI()}/foo/bar") {
            form {
                foo = "bar"
            }
        }

        def response = p.toBlocking().first()

        then:"The form was submitted correct"
        mock.verify()
        response.status == HttpStatus.OK
    }

    void 'Test simple GET request with JSON response'() {
        given:"an http client instance"
        RxHttpClientBuilder client = new TestRxHttpClientBuilder()
        def mock = client.expect {
            uri '/foo/bar'
            method "GET"
            accept 'application/json'
        }.respond {
            ok()
            json {
                title "Hello"
            }
        }

        when:
        Observable<HttpClientResponse> p = client.get("${mock.serverURI()}/foo/bar") {
            accept 'application/json'
        }
        final def response = p.toBlocking().first()

        then:
        mock.verify()
        response.status == HttpStatus.OK
        response.header("Content-Type") == 'application/json'
        response.toJson().toBlocking().first().title == "Hello"
    }


    void 'Test simple POST request with JSON body and JSON response'() {
        given:"an http client instance"
        TestRxHttpClientBuilder client = new TestRxHttpClientBuilder()
        def mock = client.expect {
            uri '/foo/bar'
            method "POST"
            contentType 'application/json'
            json {
                title "Ping"
            }
        }.respond {
            created()
            json {
                title "Pong"
            }
        }

        when:
        Observable<HttpClientResponse> p = client.post("${mock.serverURI()}/foo/bar") {
            contentType 'application/json'
            json {
                title "Ping"
            }
        }

        final def response = p.toBlocking().first()

        then:mock.verify()
        response.status == HttpStatus.CREATED
        response.header("Content-Type") == 'application/json'
        response.toJson().toBlocking().first().title == "Pong"
    }

    void 'Test simple POST request with XML body and XMLresponse'() {
        given:"an http client instance"
        TestRxHttpClientBuilder client = new TestRxHttpClientBuilder()
        def mock = client.expect {
            uri '/foo/bar'
            method HttpMethod.POST
            contentType 'application/xml'
            xml {
                title "Ping"
            }
        }.respond {
            created()
            xml {
                title "Pong"
            }
        }

        when:
        Observable<HttpClientResponse> p = client.post("${mock.serverURI()}/foo/bar") {
            xml {
                title "Ping"
            }
        }

        final def response = p.toBlocking().first()

        then:
        mock.verify()
        response.status == HttpStatus.CREATED
        response.header("Content-Type") == 'application/xml'
        response.toXml().toBlocking().first().text() == "Pong"
    }
}
