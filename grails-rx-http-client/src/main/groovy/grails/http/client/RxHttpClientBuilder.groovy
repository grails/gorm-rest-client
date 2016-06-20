package grails.http.client

import grails.http.HttpMethod
import grails.http.client.builder.HttpClientRequestBuilder
import grails.http.client.cfg.DefaultConfiguration
import grails.http.client.exceptions.HttpClientException
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelOption
import io.netty.handler.proxy.HttpProxyHandler
import io.netty.handler.proxy.Socks5ProxyHandler
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import io.reactivex.netty.client.ConnectionProviderFactory
import io.reactivex.netty.client.Host
import io.reactivex.netty.client.pool.PoolConfig
import io.reactivex.netty.client.pool.SingleHostPoolingProviderFactory
import io.reactivex.netty.protocol.http.client.HttpClient
import io.reactivex.netty.protocol.http.client.HttpClientRequest
import rx.Observable
import rx.functions.Func0
import rx.functions.Func1

import javax.annotation.PreDestroy
import javax.net.ssl.SSLEngine
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

/**
 * An asynchronous REST client that leverages Netty for non-blocking IO
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@CompileStatic
class RxHttpClientBuilder implements Closeable{


    final Configuration configuration
    final Charset charset
    final ConnectionProviderFactory connectionProviderFactory

    /**
     * Constructs a new RxHttpClientBuilder instance for the given configuration
     *
     * @param configuration The configuration
     */
    RxHttpClientBuilder(Configuration configuration = new DefaultConfiguration()) {
        // TODO: populate the pool configuration
        this(SingleHostPoolingProviderFactory.create(new PoolConfig()), configuration)
    }

    /**
     * Constructs a new RxHttpClientBuilder instance for the given configuration
     *
     * @param configuration The configuration
     */
    RxHttpClientBuilder(ConnectionProviderFactory connectionProviderFactory, Configuration configuration = new DefaultConfiguration()) {
        this.configuration = configuration
        this.connectionProviderFactory = connectionProviderFactory
        this.charset = Charset.forName(configuration.encoding)
    }


    /**
     * Executes a GET request to the given URI with an optional customizer
     *
     * @param uri The URI
     * @param customizer The customizer
     * @return A Promise to return a {@link HttpClientResponse}
     */
    Observable<HttpClientResponse> get(String uri, @DelegatesTo(HttpClientRequestBuilder) Closure customizer = null) {
        return doRequest(HttpMethod.GET, customizer, uri)
    }

    /**
     * Executes a POST request to the given URI with an optional customizer
     *
     * @param uri The URI
     * @param customizer The customizer
     * @return A Promise to return a {@link HttpClientResponse}
     */
    Observable<HttpClientResponse> post(String uri, @DelegatesTo(HttpClientRequestBuilder) Closure customizer = null) {
        return doRequest(HttpMethod.POST, customizer, uri)
    }

    /**
     * Executes a PUT request to the given URI with an optional customizer
     *
     * @param uri The URI
     * @param customizer The customizer
     * @return A Promise to return a {@link HttpClientResponse}
     */
    Observable<HttpClientResponse> put(String uri, @DelegatesTo(HttpClientRequestBuilder) Closure customizer = null) {
        return doRequest(HttpMethod.PUT, customizer, uri)
    }

    /**
     * Executes a PATCH request to the given URI with an optional customizer
     *
     * @param uri The URI
     * @param customizer The customizer
     * @return A Promise to return a {@link HttpClientResponse}
     */
    Observable<HttpClientResponse> patch(String uri, @DelegatesTo(HttpClientRequestBuilder) Closure customizer = null) {
        return doRequest(HttpMethod.PATCH, customizer, uri)
    }

    /**
     * Executes a DELETE request to the given URI with an optional customizer
     *
     * @param uri The URI
     * @param customizer The customizer
     * @return A Promise to return a {@link HttpClientResponse}
     */
    Observable<HttpClientResponse> delete(String uri, @DelegatesTo(HttpClientRequestBuilder) Closure customizer = null) {
        return doRequest(HttpMethod.DELETE, customizer, uri)
    }

    /**
     * Executes a HEAD request to the given URI with an optional customizer
     *
     * @param uri The URI
     * @param customizer The customizer
     * @return A Promise to return a {@link HttpClientResponse}
     */
    Observable<HttpClientResponse> head(String uri, @DelegatesTo(HttpClientRequestBuilder) Closure customizer = null) {
        return doRequest(HttpMethod.HEAD, customizer, uri)
    }

    /**
     * Executes a OPTIONS request to the given URI with an optional customizer
     *
     * @param uri The URI
     * @param customizer The customizer
     * @return A Promise to return a {@link HttpClientResponse}
     */
    Observable<HttpClientResponse> options(String uri, @DelegatesTo(HttpClientRequestBuilder) Closure customizer = null) {
        return doRequest(HttpMethod.OPTIONS, customizer, uri)
    }

    protected Observable<HttpClientResponse> doRequest(HttpMethod httpMethod, Closure customizer, String uri) {
        final URI uriObject = new URI(uri)

        SslContext sslCtx = buildSslContext(uriObject)

        def address = createAddressForURI(uriObject)
        HttpClient client = HttpClient.newClient(connectionProviderFactory, Observable.just(new Host(address)))
                                        .readTimeOut(configuration.readTimeout, TimeUnit.MILLISECONDS)

        if(sslCtx != null) {
            client = client.secure({ ByteBufAllocator allocator ->
                return sslCtx.newEngine(allocator)
            } as Func1<ByteBufAllocator, SSLEngine>)
        }
        for(entry in configuration.channelOptions) {
            client = client
                        .channelOption(ChannelOption.newInstance(entry.key), entry.value)
        }


        def proxy = configuration.proxy
        if(proxy != null) {
            def type = proxy.type().name().toLowerCase()
            String username = System.getProperty("${type}.proxyUser")
            String password = System.getProperty("${type}.proxyPassword")

            client.addChannelHandlerLast("proxy", {
                if(username && password) {
                    switch(proxy.type()) {
                        case Proxy.Type.SOCKS:
                            return new Socks5ProxyHandler(proxy.address(), username, password)
                            break
                        default:
                            return new HttpProxyHandler(proxy.address(), username, password)
                            break

                    }
                }
                else {
                    switch(proxy.type()) {
                        case Proxy.Type.SOCKS:
                            return new Socks5ProxyHandler(proxy.address())
                            break
                        default:
                            return new HttpProxyHandler(proxy.address())
                            break
                    }
                }
            } as Func0<ChannelHandler>)

        }

        switch (httpMethod) {
            case HttpMethod.GET:
                return customizeRequest(client.createGet(uriObject.path), customizer ).map { io.reactivex.netty.protocol.http.client.HttpClientResponse res ->
                    return new HttpClientResponse(res)
                }
            break
            case HttpMethod.POST:
                return customizeRequest(client.createPost(uriObject.path), customizer).map { io.reactivex.netty.protocol.http.client.HttpClientResponse res ->
                    return new HttpClientResponse(res)
                }
            break
            case HttpMethod.PUT:
                return customizeRequest(client.createPut(uriObject.path), customizer).map { io.reactivex.netty.protocol.http.client.HttpClientResponse res ->
                    return new HttpClientResponse(res)
                }
            break
            case HttpMethod.DELETE:
                return customizeRequest(client.createDelete(uriObject.path), customizer).map { io.reactivex.netty.protocol.http.client.HttpClientResponse res ->
                    return new HttpClientResponse(res)
                }
            break
            case HttpMethod.PATCH:
                return customizeRequest(client.createPatch(uriObject.path), customizer).map { io.reactivex.netty.protocol.http.client.HttpClientResponse res ->
                    return new HttpClientResponse(res)
                }
            break
            case HttpMethod.OPTIONS:
                return customizeRequest(client.createOptions(uriObject.path), customizer).map { io.reactivex.netty.protocol.http.client.HttpClientResponse res ->
                    return new HttpClientResponse(res)
                }
            break
        }
        throw new UnsupportedOperationException("Unsupported HTTP method $httpMethod")

    }

    @CompileDynamic
    HttpClientRequest customizeRequest(HttpClientRequest<ByteBuf, ByteBuf> request, Closure configurer) {
        if(configurer != null) {
            HttpClientRequestBuilder requestBuilder = new HttpClientRequestBuilder(request, charset)
            configurer.setDelegate(requestBuilder)
            configurer.call()
            request = requestBuilder.request
        }

        return request
    }

    private InetSocketAddress createAddressForURI(URI uriObject) {
        String scheme = uriObject.scheme
        if(scheme == 'http') {
            new InetSocketAddress(uriObject.host, uriObject.port > -1 ? uriObject.port : 8080)
        }
        else if(scheme == 'https') {
            new InetSocketAddress(uriObject.host, uriObject.port > -1 ? uriObject.port : 8443)
        }
        else {
            throw new HttpClientException("Invalid scheme $scheme")
        }
    }


    protected SslContext buildSslContext(URI uriObject) {
        final SslContext sslCtx
        if (uriObject.scheme == 'https') {
            sslCtx = buildSslContext(configuration)
        } else {
            sslCtx = null
        }
        sslCtx
    }


    /**
     * Builds an {@link SslContext} from the {@link Configuration}
     *
     * @param configuration The configuration instance
     * @return The {@link SslContext} instance
     */
    protected SslContext buildSslContext(Configuration configuration) {
        SslContextBuilder.forClient()
                .sslProvider(configuration.sslProvider)
                .sessionCacheSize(configuration.sslSessionCacheSize)
                .sessionTimeout(configuration.sslSessionTimeout)
                .trustManager(configuration.sslTrustManagerFactory)
                .build()
    }


    /**
     * Close down the client and shutdown the backing thread pool
     *
     * @throws IOException
     */
    @Override
    @PreDestroy
    void close() throws IOException {
        //

    }

}
