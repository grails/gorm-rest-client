package grails.http.client

import grails.http.HttpStatus
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import groovy.util.slurpersupport.GPathResult
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufHolder
import io.netty.buffer.ByteBufInputStream
import io.netty.buffer.Unpooled
import rx.Observable
import rx.functions.Func1

import java.nio.charset.Charset

/**
 * Represents an HTTP response
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@CompileStatic
class HttpClientResponse {

    final io.reactivex.netty.protocol.http.client.HttpClientResponse nettyResponse
    final Charset charset

    HttpClientResponse(io.reactivex.netty.protocol.http.client.HttpClientResponse nettyResponse) {
        this(nettyResponse, "UTF-8")
    }

    HttpClientResponse(io.reactivex.netty.protocol.http.client.HttpClientResponse nettyResponse, String encoding) {
        this(nettyResponse, Charset.forName(encoding))
    }

    HttpClientResponse(io.reactivex.netty.protocol.http.client.HttpClientResponse nettyResponse, Charset charset) {
        this.nettyResponse = nettyResponse
        this.charset = charset
    }
    /**
     * @return The JSON representation of the response body
     */
    Observable toJson() {
        nettyResponse.content.toList().map({ List<ByteBuf> o ->
            ByteBuf byteBuf = Unpooled.wrappedBuffer(o as ByteBuf[])
            ByteBufInputStream inputStream = null

            try {
                inputStream = new ByteBufInputStream(byteBuf)
                Reader reader = new InputStreamReader(inputStream, charset)
                return new JsonSlurper().parse(reader)
            } finally {
                inputStream?.close()
            }
        })
    }

    /**
     * @return An XML representation for the response body
     */
    Observable<GPathResult> toXml() {
        nettyResponse.content.toList().map({ List<ByteBuf> o ->
            ByteBuf byteBuf = Unpooled.wrappedBuffer(o as ByteBuf[])
            ByteBufInputStream inputStream = null

            try {
                inputStream = new ByteBufInputStream(byteBuf)
                Reader reader = new InputStreamReader(inputStream, charset)
                return new XmlSlurper().parse(reader)
            } finally {
                inputStream?.close()
            }
        })
    }

    Observable getBody() {
        return nettyResponse.content
    }

    Observable<String> toText(Charset charset = this.charset) {
        nettyResponse.content.toList().map({ List<ByteBuf> o ->
            ByteBuf byteBuf = Unpooled.wrappedBuffer(o as ByteBuf[])
            return byteBuf.toString(charset)

        })
    }

    /**
     * Obtain a value of a header
     *
     * @param name The header name
     * @return The value
     */
    String getHeader(String name) {
        nettyResponse.getHeader(name)
    }

    /**
     * @return The header names
     */
    Set<String> getHeaderNames() {
        nettyResponse.getHeaderNames()
    }
    /**
     * Obtain a value of a header
     *
     * @param name The header name
     * @return The value
     */
    String header(String name) {
        nettyResponse.getHeader(name)
    }

    /**
     * @return The status code of the response
     */
    int getStatusCode() {
        nettyResponse.status.code()
    }

    /**
     * @return The reason message
     */
    String getStatusReason() {
        nettyResponse.status.reasonPhrase()
    }

    /**
     * @return The returned http status object
     */
    HttpStatus getStatus() {
        HttpStatus.valueOf(nettyResponse.status.code())
    }

    @Override
    String toString() {
        nettyResponse.toString()
    }
}

