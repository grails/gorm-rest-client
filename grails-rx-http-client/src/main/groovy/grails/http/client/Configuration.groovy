package grails.http.client

import groovy.transform.CompileStatic
import io.netty.channel.ChannelHandler
import io.netty.handler.codec.http.HttpClientCodec
import io.netty.handler.codec.http.HttpVersion
import io.netty.handler.logging.LogLevel
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslProvider
import io.netty.handler.ssl.util.InsecureTrustManagerFactory

import javax.net.ssl.TrustManagerFactory
import java.util.concurrent.ThreadFactory

/**
 * Configuration for the HTTP client
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@CompileStatic
trait Configuration {

    /**
     * The default wire log level
     */
    LogLevel wireLogLevel
    /**
     * The wire logger name
     */
    String wireLogName = "grails.http.client"
    /**
     * The encoding to use
     */
    String encoding = "UTF-8"

    /**
     * The read timeout
     */
    int readTimeout = -1

    /**
     * The SSL provider to use
     */
    SslProvider sslProvider = SslContext.defaultClientProvider()

    /**
     * The default session cache size
     */
    long sslSessionCacheSize

    /**
     * The SSL timeout period
     */
    long sslSessionTimeout

    /**
     * The default trust manager factory
     */
    TrustManagerFactory sslTrustManagerFactory = InsecureTrustManagerFactory.INSTANCE

    /**
     * Options for the netty channel
     */
    Map<String, Object> channelOptions = [:]

    /**
     * The proxy to use. For authentication specify http.proxyUser and http.proxyPassword system properties
     *
     * Alternatively configure a java.net.ProxySelector
     */
    Proxy proxy

    /**
     * The HTTP version to use
     */
    HttpVersion httpVersion = HttpVersion.HTTP_1_1

}