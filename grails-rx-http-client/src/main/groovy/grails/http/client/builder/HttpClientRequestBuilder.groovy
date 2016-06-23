package grails.http.client.builder

import grails.http.HttpHeader
import grails.http.MediaType
import groovy.json.StreamingJsonBuilder
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import groovy.util.slurpersupport.GPathResult
import groovy.xml.StreamingMarkupBuilder
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufOutputStream
import io.netty.buffer.Unpooled
import io.netty.handler.codec.base64.Base64
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaders
import io.netty.handler.codec.http.multipart.HttpPostRequestEncoder
import io.netty.handler.codec.http.multipart.MemoryFileUpload
import io.reactivex.netty.protocol.http.client.HttpClientRequest
import io.reactivex.netty.protocol.http.client.HttpClientResponse
import rx.Observable
import rx.Subscriber

import java.nio.charset.Charset

/**
 * @author Graeme Rocher
 * @since 1.0
 */
@CompileStatic
@Slf4j
class HttpClientRequestBuilder {

    Observable<HttpClientResponse> request
    Charset charset

    HttpClientRequestBuilder(HttpClientRequest request, Charset charset) {
        this.request = request
        this.charset = charset
    }
    /**
     * Sets the URI of the request
     * @param uri The uriTemplate of the request
     * @return the request
     */
    HttpClientRequestBuilder uri(CharSequence uri) {
        if (request instanceof HttpClientRequest) {
            request = ((HttpClientRequest) request).setUri(uri.toString())
        }
        else {
            throw new IllegalStateException("Body already written. Write the body last after setting any headers or request properties.")
        }
        return this
    }

    /**
     * Sets the method of the request
     * @param method The HTTP method
     * @return This request
     */
    HttpClientRequestBuilder method(CharSequence method) {
        if (request instanceof HttpClientRequest) {
            request = ((HttpClientRequest) request).setMethod(io.netty.handler.codec.http.HttpMethod.valueOf(method.toString()))
        }
        else {
            throw new IllegalStateException("Body already written. Write the body last after setting any headers or request properties.")
        }

        return this
    }

    /**
     * Used to configure BASIC authentication. Example:
     *
     * <pre><code>
     * builder.put("http://..") {*      auth "myuser", "mypassword"
     *}* </code></pre>
     *
     * @param username The username
     * @param password The password
     * @return The request
     */
    HttpClientRequestBuilder auth(String username, String password) {
        String usernameAndPassword = "$username:$password"
        def encoded = Base64.encode(Unpooled.wrappedBuffer(usernameAndPassword.bytes)).toString(charset)
        header HttpHeaderNames.AUTHORIZATION, "Basic $encoded".toString()
        return this
    }
    /**
     * Sets the content type for the request
     *
     * @param contentType The content type
     */
    HttpClientRequestBuilder contentType(CharSequence contentType) {
        header(HttpHeaderNames.CONTENT_TYPE, contentType)
        return this
    }

    /**
     * Sets a request header
     *
     * @param name The name of the header
     * @param value The value of the header
     * @return This request
     */
    HttpClientRequestBuilder header(CharSequence name, value) {
        if (request instanceof HttpClientRequest) {
            request = ((HttpClientRequest) request).setHeader(name, value)
        }
        else {
            throw new IllegalStateException("Body already written. Write the body last after setting any headers or request properties.")
        }

        return this
    }

    /**
     * Adds JSON to the body of the request
     * @param callable The callable that defines the JSON
     * @return
     */
    HttpClientRequestBuilder json(@DelegatesTo(StreamingJsonBuilder) Closure callable) {
        header(HttpHeader.CONTENT_TYPE, MediaType.JSON)
        if (request instanceof HttpClientRequest) {
            request = ((HttpClientRequest) request).writeContent(
                    createBodyWriter { Writer writer ->
                        def jsonBuilder = new StreamingJsonBuilder(writer)
                        jsonBuilder.call callable
                    }
            )
        }
        else {
            throw new IllegalStateException("Body already written. Write the body last after setting any headers or request properties.")
        }

        return this
    }

    /**
     * Adds JSON to the body of the request
     * @param array The JSON array
     * @return This request
     */
    HttpClientRequestBuilder json(List array) {
        header(HttpHeader.CONTENT_TYPE, MediaType.JSON)
        if (request instanceof HttpClientRequest) {
            request = ((HttpClientRequest) request).writeContent(
                    createBodyWriter { Writer writer ->
                        def builder = new StreamingJsonBuilder(writer)
                        builder.call(array)
                    }
            )
        }
        else {
            throw new IllegalStateException("Body already written. Write the body last after setting any headers or request properties.")
        }

        return this
    }

    /**
     * Adds JSON to the body of the request
     * @param json The JSON as a map
     * @return This request
     */
    HttpClientRequestBuilder json(Map json) {
        header(HttpHeader.CONTENT_TYPE, MediaType.JSON)
        if (request instanceof HttpClientRequest) {
            request = ((HttpClientRequest) request).writeContent(

                    createBodyWriter { Writer writer ->
                        def builder = new StreamingJsonBuilder(writer)
                        builder.call(json)
                    }
            )
        }
        else {
            throw new IllegalStateException("Body already written. Write the body last after setting any headers or request properties.")
        }

        return this
    }

    /**
     * Adds JSON to the body of the request
     * @param json The JSON as a map
     * @return This request
     */
    HttpClientRequestBuilder json(String json) {
        header(HttpHeader.CONTENT_TYPE, MediaType.JSON)
        if (request instanceof HttpClientRequest) {
            request = ((HttpClientRequest) request).writeContent(
                    createBodyWriter { Writer writer ->
                        writer.write(json)
                    }
            )
        }
        return this
    }

    /**
     * Sets the body of the request to the XML string argument.
     *
     * @param xml The XML to be used as the body of the request
     * @return This customizer
     */
    HttpClientRequestBuilder xml(String xml) {
        header(HttpHeader.CONTENT_TYPE, MediaType.XML)
        if (request instanceof HttpClientRequest) {
            request = ((HttpClientRequest) request).writeContent(
                    createBodyWriter { Writer writer ->
                        writer.write(xml)
                    }
            )
        }
        else {
            throw new IllegalStateException("Body already written. Write the body last after setting any headers or request properties.")
        }

        return this
    }

    /**
     * Sets the body of the request to the XML defined by the closure. Uses {@link groovy.xml.StreamingMarkupBuilder} to produce the XML
     *
     * @param closure The closure that defines the XML
     * @return This customizer
     */
    HttpClientRequestBuilder xml(@DelegatesTo(StreamingMarkupBuilder) Closure closure) {
        header(HttpHeader.CONTENT_TYPE, MediaType.XML)
        if (request instanceof HttpClientRequest) {
            request = ((HttpClientRequest) request).writeContent(
                    createBodyWriter { Writer writer ->
                        def b = new StreamingMarkupBuilder()
                        Writable markup = (Writable) b.bind(closure)
                        markup.writeTo(writer)

                    }
            )
        }
        else {
            throw new IllegalStateException("Body already written. Write the body last after setting any headers or request properties.")
        }

        return this
    }

    /**
     * Sets the body of the request to the XML GPathResult argument.
     *
     * @param xml The XML to be used as the body of the request
     * @return This customizer
     */
    HttpClientRequestBuilder xml(GPathResult xml) {
        header(HttpHeader.CONTENT_TYPE, MediaType.XML)
        if (request instanceof HttpClientRequest) {
            request = ((HttpClientRequest) request).writeContent(
                    createBodyWriter { Writer writer ->
                        xml.writeTo(writer)

                    }
            )
        }
        else {
            throw new IllegalStateException("Body already written. Write the body last after setting any headers or request properties.")
        }

        return this
    }

    /**
     * Sets the Accept HTTP header to the given value. Example:
     *
     * <pre><code>
     * restBuilder.get("http://..") {*      accept "application/xml"
     *}* </code></pre>
     *
     * @param contentTypes The content types
     * @return The customizer
     */
    HttpClientRequestBuilder accept(CharSequence... contentTypes) {
        header HttpHeaderNames.ACCEPT, contentTypes.join(',')
        return this
    }

    /**
     * Sets the Authorization HTTP header to the given value. Used typically to pass OAuth access tokens.
     *
     * <pre><code>
     * builder.put("http://..") {*      auth myToken
     *}* </code></pre>
     *
     * @param accessToken The access token
     * @return The customizer
     */
    HttpClientRequestBuilder auth(CharSequence accessToken) {
        header HttpHeaderNames.AUTHORIZATION, accessToken
        return this
    }

//  TODO: re-enable these methods if RxNetty adds support for multipart uploads
//    /**
//     * Builds a form
//     * @param formDefinition The form definition
//     * @return this object
//     */
//    HttpClientRequestBuilder form(@DelegatesTo(FormDataBuilder) Closure formDefinition) {
//        if(formDefinition != null) {
//
//            HttpPostRequestEncoder encoder = new HttpPostRequestEncoder(request, false)
//            formDefinition.delegate = new FormDataBuilder(encoder, charset)
//            formDefinition.call()
//            wrapped = encoder.finalizeRequest()
//        }
//        return this
//    }
//
//    /**
//     * Builds a multipart form
//     * @param formDefinition The form definition
//     * @return this object
//     */
//    HttpClientRequestBuilder multipart(@DelegatesTo(MultipartBuilder) Closure formDefinition) {
//        if(formDefinition != null) {
//
//            HttpPostRequestEncoder encoder = new HttpPostRequestEncoder(request, true)
//            formDefinition.delegate = new MultipartBuilder(encoder, charset)
//            formDefinition.call()
//            wrapped = encoder.finalizeRequest()
//        }
//        return this
//    }

    protected Observable createBodyWriter(Closure callable) {
        Observable.create({ Subscriber<ByteBuf> subscriber ->
            ByteBuf byteBuf = Unpooled.buffer()
            try {
                def writer = new OutputStreamWriter(new ByteBufOutputStream(byteBuf), charset)
                callable.call(writer)
                writer.flush()

                subscriber.onNext(byteBuf)
            } catch (Throwable e) {
                log.error "Error writing JSON to request body: $e.message", e
                subscriber.onError(e)
            }
            finally {
                byteBuf.release()
                subscriber.onCompleted()
            }

        } as Observable.OnSubscribe<ByteBuf>)
    }

    /**
     * Helps with building multipart requests
     */
    static class MultipartBuilder extends FormDataBuilder {
        static final List TEXT_TYPES = ['xml', 'json', 'txt', 'yml', 'csv', 'html', 'rss', 'hal', 'svg']
        static final Map COMMON_CONTENT_TYPES = [
                xml : 'application/xml',
                json: 'application/json',
                hal : 'application/hal+json',
                rss : 'application/rss+xml',
                yml : 'application/yml',
                csv : 'text/csv',
                css : 'text/css',
                html: 'text/html',
                txt : 'text/plain',
                bin : 'application/octet-stream',
                jpg : 'image/jpeg',
                gif : 'image/gif',
                png : 'image/png',
                svg : 'image/svg+xml'
        ]

        MultipartBuilder(HttpPostRequestEncoder encoder, Charset charset) {
            super(encoder, charset, true)
        }

        /**
         * Adds a file to the request body for the given name
         *
         * @param name The name within the multpart body
         * @param file The file itself
         * @param contentType The content type of the file. Defaults to `text/plain` for text content if not specified
         * @param isText Whether the file is text or binary
         *
         * @return This builder
         */
        FormDataBuilder file(String name, File file, String contentType = guessContentType(file.name), boolean isText = guessIsText(file.name)) {
            encoder.addBodyFileUpload(name, file, contentType, isText)
            return this
        }

        /**
         * Adds a file to the request body for the given name, filename and bytes
         * @param name The name within the multipart body
         * @param filename The file name
         * @param bytes The bytes of the file
         * @param contentType The content type, defaults to 'application/octet-stream'
         * @return This builder
         */
        FormDataBuilder file(String name, String filename, byte[] bytes, String contentType = guessContentType(filename, true)) {
            def upload = new MemoryFileUpload(name, filename, contentType, "binary", null, bytes.length)
            upload.setContent(Unpooled.wrappedBuffer(bytes))
            encoder.addBodyHttpData(upload)
            return this
        }

        /**
         * Adds a file to the request body for the given name, filename and bytes
         * @param name The name within the multipart body
         * @param filename The file name
         * @param bytes The bytes of the file as an input stream
         * @param contentType The content type, defaults to 'application/octet-stream'
         * @return This builder
         */
        FormDataBuilder file(String name, String filename, InputStream bytes, int length, String contentType = guessContentType(filename, true)) {
            def upload = new MemoryFileUpload(name, filename, contentType, "binary", null, length)
            upload.setContent(bytes)
            encoder.addBodyHttpData(upload)
            return this
        }
        /**
         * Adds a file for the given name, filename and text
         *
         * @param name The name within the multipart body
         * @param filename The name of the file
         * @param body The body of the file as text
         * @param contentType The content type, defaults to `text/plain`
         * @param charset The charset
         * @return This builder
         */
        FormDataBuilder file(String name, String filename, CharSequence body, String contentType = guessContentType(filename), Charset charset = this.charset) {
            def upload = new MemoryFileUpload(name, filename, contentType, null, charset, body.length())
            upload.setContent(Unpooled.copiedBuffer(body, charset))
            encoder.addBodyHttpData(upload)
            return this
        }

        protected String guessContentType(String filename, boolean binary = false) {
            def i = filename.lastIndexOf('.')
            String contentType = null
            if (i > -1) {
                contentType = COMMON_CONTENT_TYPES.get(filename.substring(i + 1))
            }

            if (contentType == null) {
                return binary ? 'application/octet-stream' : 'text/plain'
            } else {
                return contentType
            }
        }

        protected boolean guessIsText(String filename) {
            def i = filename.lastIndexOf('.')
            if (i > -1) {
                return TEXT_TYPES.contains(filename.substring(i + 1))
            }
            return false
        }

        @Override
        void setProperty(String property, Object newValue) {
            if (multipart && newValue instanceof File) {
                def f = (File) newValue
                encoder.addBodyFileUpload(property, f, guessContentType(f.name), guessIsText(f.name))
            } else {
                super.setProperty(property, newValue)
            }
        }
    }

    static class FormDataBuilder {

        final HttpPostRequestEncoder encoder
        final boolean multipart
        final Charset charset

        FormDataBuilder(HttpPostRequestEncoder encoder, Charset charset, boolean multipart = false) {
            this.encoder = encoder
            this.multipart = multipart
            this.charset = charset
        }

        @Override
        void setProperty(String property, Object newValue) {
            encoder.addBodyAttribute(property, newValue.toString())
        }

        @Override
        @CompileDynamic
        Object invokeMethod(String name, Object args) {
            if (args && args.size() == 1) {
                encoder.addBodyAttribute(name, args[0].toString())
            }
            throw new MissingMethodException(name, getClass(), args)
        }
    }


}
