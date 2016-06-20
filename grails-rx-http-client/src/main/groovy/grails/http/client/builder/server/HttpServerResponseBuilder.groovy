package grails.http.client.builder.server

import groovy.json.StreamingJsonBuilder
import groovy.transform.CompileStatic
import groovy.util.slurpersupport.GPathResult
import groovy.xml.StreamingMarkupBuilder
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpResponseStatus
import io.reactivex.netty.protocol.http.server.HttpServerResponse
import rx.Observable

import java.nio.charset.Charset

/**
 * A builder for populating {@link HttpServerResponse} objects, useful only in testing scenarios
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
class HttpServerResponseBuilder {
    final HttpServerResponse response
    final Charset charset


    HttpServerResponseBuilder(HttpServerResponse httpMessage, Charset charset) {
        this.response = httpMessage
        this.charset = charset
    }

    /**
     * Sets an ok status
     *
     * @return This builder
     */

    HttpServerResponseBuilder ok() {
        this.response.setStatus(HttpResponseStatus.OK)
        return this
    }

    /**
     * Sets a UNPROCESSABLE_ENTITY status
     *
     * @return This builder
     */

    HttpServerResponseBuilder unprocessable() {
        this.response.setStatus(HttpResponseStatus.UNPROCESSABLE_ENTITY)
        return this
    }
    /**
     * Sets a NO_CONTENT status
     *
     * @return This builder
     */

    HttpServerResponseBuilder noContent() {
        this.response.setStatus(HttpResponseStatus.NO_CONTENT)
        return this
    }

    /**
     * Sets an UNAUTHORIZED status
     *
     * @return This builder
     */
    HttpServerResponseBuilder unauthorized() {
        this.response.setStatus(HttpResponseStatus.UNAUTHORIZED)
        return this
    }

    /**
     * Sets an FORBIDDEN status
     *
     * @return This builder
     */
    HttpServerResponseBuilder forbidden() {
        this.response.setStatus(HttpResponseStatus.FORBIDDEN)
        return this
    }

    /**
     * Sets a created status
     *
     * @return The created status
     */
    HttpServerResponseBuilder created() {
        this.response.setStatus(HttpResponseStatus.CREATED)
        return this
    }


    /**
     * Sets not found status
     *
     * @return The not found status
     */
    HttpServerResponseBuilder notFound() {
        this.response.setStatus(HttpResponseStatus.NOT_FOUND)
        return this
    }

    /**
     * Sets the status of the response
     *
     * @param code The status code
     * @return This builder
     */
    HttpServerResponseBuilder status(int code) {
        this.response.setStatus(HttpResponseStatus.valueOf(code))
        return this
    }

    /**
     * Sets the status of the response
     *
     * @param code The status code
     * @return This builder
     */
    HttpServerResponseBuilder status(HttpResponseStatus status) {
        this.response.setStatus(status)
        return this
    }
    /**
     * Sets the content type for the request
     *
     * @param contentType The content type
     */
    HttpServerResponseBuilder contentType(CharSequence contentType) {
        response.setHeader(HttpHeaderNames.CONTENT_TYPE, contentType)
        return this
    }

    /**
     * Sets a request header
     *
     * @param name The name of the header
     * @param value The value of the header
     * @return This request
     */
    HttpServerResponseBuilder header(CharSequence name, value) {
        response.setHeader(name, value)
        return this
    }


    /**
     * Adds JSON to the body of the request
     * @param callable The callable that defines the JSON
     * @return
     */
    HttpServerResponseBuilder json(@DelegatesTo(StreamingJsonBuilder) Closure callable) {
        def str = new StringWriter()
        defaultJsonContentType()
        StreamingJsonBuilder builder = new StreamingJsonBuilder(str)
        builder.call(callable)
        response.writeStringAndFlushOnEach(Observable.just(str.toString())).toBlocking().first()
        return this
    }

    /**
     * Adds JSON to the body of the request
     * @param array The JSON array
     * @return This request
     */
    HttpServerResponseBuilder json(List array) {
        def str = new StringWriter()
        defaultJsonContentType()
        StreamingJsonBuilder builder = new StreamingJsonBuilder(str)
        builder.call(array)
        response.writeStringAndFlushOnEach(Observable.just(str.toString())).toBlocking().first()
        return this
    }

    /**
     * Adds JSON to the body of the request
     * @param json The JSON as a map
     * @return This request
     */
    HttpServerResponseBuilder json(Map json) {
        def str = new StringWriter()
        defaultJsonContentType()
        StreamingJsonBuilder builder = new StreamingJsonBuilder(str)
        builder.call(json)
        response.writeStringAndFlushOnEach(Observable.just(str.toString())).toBlocking().first()
        return this
    }

    /**
     * Adds JSON to the body of the request
     * @param json The JSON as a map
     * @return This request
     */
    HttpServerResponseBuilder json(String json) {
        defaultJsonContentType()
        response.writeStringAndFlushOnEach(Observable.just(json)).toBlocking().first()
        return (HttpServerResponseBuilder)this
    }

    /**
     * Sets the body of the request to the XML string argument.
     *
     * @param xml The XML to be used as the body of the request
     * @return This customizer
     */
    HttpServerResponseBuilder xml(String xml) {
        response.setHeader(HttpHeaderNames.CONTENT_TYPE, "application/xml")
        response.writeStringAndFlushOnEach(Observable.just(xml)).toBlocking().first()
        return this
    }

    /**
     * Sets the body of the request to the XML defined by the closure. Uses {@link groovy.xml.StreamingMarkupBuilder} to produce the XML
     *
     * @param closure The closure that defines the XML
     * @return This customizer
     */
    HttpServerResponseBuilder xml(@DelegatesTo(StreamingMarkupBuilder)Closure closure) {
        response.setHeader(HttpHeaderNames.CONTENT_TYPE, "application/xml")
        StringWriter writer = new StringWriter()
        def b = new StreamingMarkupBuilder()
        Writable markup = (Writable)b.bind(closure)
        markup.writeTo(writer)
        writer.flush()
        response.writeStringAndFlushOnEach(Observable.just(writer.toString())).toBlocking().first()
        return this
    }

    /**
     * Sets the body of the request to the XML GPathResult argument.
     *
     * @param xml The XML to be used as the body of the request
     * @return This customizer
     */
    HttpServerResponseBuilder xml(GPathResult xml) {
        response.setHeader(HttpHeaderNames.CONTENT_TYPE, "application/xml")
        StringWriter writer = new StringWriter()
        xml.writeTo(writer)
        writer.flush()
        response.writeStringAndFlushOnEach(Observable.just(writer.toString())).toBlocking().first()
        return this
    }


    protected void defaultJsonContentType() {
        defaultContentType("application/json")
    }

    protected void defaultContentType(String contentType) {

        if (!response.getHeader(HttpHeaderNames.CONTENT_TYPE)) {
            response.setHeader(HttpHeaderNames.CONTENT_TYPE, contentType)
        }
    }
}
