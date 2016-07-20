package org.grails.datastore.rx.rest

import com.damnhandy.uri.template.UriTemplate
import grails.gorm.rx.RxEntity
import grails.gorm.rx.rest.interceptor.InterceptorContext
import grails.gorm.rx.rest.interceptor.RequestInterceptor
import grails.http.HttpMethod
import grails.http.MediaType
import grails.http.client.RxHttpClientBuilder
import grails.http.client.cfg.DefaultConfiguration
import grails.http.client.exceptions.HttpClientException
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.ByteBufInputStream
import io.netty.buffer.ByteBufOutputStream
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandler
import io.netty.handler.codec.base64.Base64
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.logging.LogLevel
import io.netty.handler.proxy.HttpProxyHandler
import io.netty.handler.proxy.Socks5ProxyHandler
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.SslProvider
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import io.reactivex.netty.client.ConnectionProviderFactory
import io.reactivex.netty.client.Host
import io.reactivex.netty.protocol.http.client.HttpClient
import io.reactivex.netty.protocol.http.client.HttpClientRequest
import io.reactivex.netty.protocol.http.client.HttpClientResponse
import org.bson.codecs.Codec
import org.bson.codecs.configuration.CodecRegistry
import org.grails.datastore.bson.codecs.BsonPersistentEntityCodec
import org.grails.datastore.bson.json.JsonReader
import org.grails.datastore.bson.json.JsonWriter
import org.grails.datastore.gorm.GormValidateable
import org.grails.datastore.gorm.validation.constraints.registry.DefaultValidatorRegistry
import org.grails.datastore.mapping.config.ConfigurationUtils
import org.grails.datastore.mapping.core.DatastoreUtils
import org.grails.datastore.mapping.core.connections.ConnectionSource
import org.grails.datastore.mapping.core.connections.ConnectionSources
import org.grails.datastore.mapping.core.connections.ConnectionSourcesInitializer
import org.grails.datastore.mapping.core.connections.InMemoryConnectionSources
import org.grails.datastore.mapping.core.connections.SingletonConnectionSources
import org.grails.datastore.mapping.model.DatastoreConfigurationException
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.query.Query
import org.grails.datastore.mapping.reflect.EntityReflector
import org.grails.datastore.mapping.validation.ValidationErrors
import org.grails.datastore.mapping.validation.ValidationException
import org.grails.datastore.rx.AbstractRxDatastoreClient
import org.grails.datastore.rx.RxDatastoreClient
import org.grails.datastore.rx.batch.BatchOperation
import org.grails.datastore.rx.bson.CodecsRxDatastoreClient
import org.grails.datastore.rx.query.QueryState
import org.grails.datastore.rx.rest.api.RxRestGormInstanceApi
import org.grails.datastore.rx.rest.api.RxRestGormStaticApi
import org.grails.datastore.rx.rest.codecs.ContextAwareCodec
import org.grails.datastore.rx.rest.config.RestClientMappingContext
import org.grails.datastore.rx.rest.config.Settings
import org.grails.datastore.rx.rest.connections.RestConnectionSourceFactory
import org.grails.datastore.rx.rest.connections.RestConnectionSourceSettings
import org.grails.datastore.rx.rest.connections.RestConnectionSourceSettingsBuilder
import org.grails.datastore.rx.rest.query.BsonRxRestQuery
import org.grails.datastore.rx.rest.query.SimpleRxRestQuery
import org.grails.gorm.rx.api.RxGormEnhancer
import org.springframework.core.convert.converter.Converter
import org.springframework.core.env.PropertyResolver
import org.springframework.validation.Errors
import rx.Observable
import rx.Subscriber
import rx.functions.Func0
import rx.functions.Func1
import rx.functions.Func2

import javax.net.ssl.SSLEngine
import javax.net.ssl.TrustManagerFactory
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit

/**
 * An RxGORM implementation that backs onto a backend REST server
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
@Slf4j
class RxRestDatastoreClient extends AbstractRxDatastoreClient implements CodecsRxDatastoreClient, Settings {
    private static final BSON_QUERY_TYPE = "bson"
    /**
     * The {@link ConnectionProviderFactory} to use to create connections
     */
    final ConnectionProviderFactory connectionProviderFactory

    /**
     * The default read timeout
     */
    final int readTimeout

    /**
     * The default log level
     */
    final LogLevel loggerLevel

    /**
     * The default log level
     */
    final String loggerName
    /**
     * The default client host to connect to
     */
    final Observable<Host> hosts

    /**
     * The {@link CodecRegistry}
     */
    final CodecRegistry codecRegistry

    /**
     * The username to use for BASIC auth
     */
    final String username

    /**
     * The password to use for BASIC auth
     */
    final String password

    /**
     * The encoding to use
     */
    final Charset charset

    /**
     * The name of the order parameter to use
     */
    final String orderParameter

    /**
     * The name of the query parameter to use
     */
    final String queryParameter

    /**
     * The name of the offset parameter to use
     */
    final String offsetParameter

    /**
     * The name of the max parameter to use
     */
    final String maxParameter

    /**
     * The name of the sort parameter to use
     */
    final String sortParameter

    /**
     * The name of the expand parameter to use
     */
    final String expandParameter

    /**
     * The default allowable set of parameter names
     */
    final Set<String> defaultParameterNames

    /**
     * A low level client for non entity related operations
     */
    final RxHttpClientBuilder rxHttpClientBuilder

    /**
     * The interceptors to use
     */
    final List<RequestInterceptor> interceptors = []

    /**
     * The query type to use
     */
    final Class<? extends SimpleRxRestQuery> queryType

    /**
     * The default method to use for updates. Defaults to PUT
     */
    final HttpMethod defaultUpdateMethod

    /**
     * Whether communication is over a security HTTPS connection
     */
    final boolean isSecure

    /**
     * The SSL provider
     */
    final SslProvider sslProvider

    /**
     * The default session cache size
     */
    final long sslSessionCacheSize

    /**
     * The SSL timeout period
     */
    final long sslSessionTimeout

    /**
     * The default trust manager factory
     */
    final TrustManagerFactory sslTrustManagerFactory

    final SslContext sslContext

    /**
     * The proxies to use to connect
     */
    final List<Proxy> proxies

    protected final boolean allowBlockingOperations

    final RestConnectionSourceSettings settings

    RxRestDatastoreClient(ConnectionSources<ConnectionProviderFactory, RestConnectionSourceSettings> connectionSources, RestClientMappingContext mappingContext) {
        super(connectionSources, mappingContext)

        ConnectionSource<ConnectionProviderFactory, RestConnectionSourceSettings> defaultConnectionSource = connectionSources.defaultConnectionSource
        RestConnectionSourceSettings settings = defaultConnectionSource.settings

        List<String> hosts = settings.hosts
        this.isSecure = settings.isSecure()

        List<SocketAddress> socketAddresses = hosts.collect() { String host ->
            URI uri = new URI(host)
            int port = (uri.port > -1 ? uri.port : (isSecure ? 443 : 80))
            return (SocketAddress)new InetSocketAddress(uri.host, port)
        }


        def hostUri = new URI(hosts.find { String o -> o })
        def proxyList = settings.proxies ?: ProxySelector.getDefault()?.select(hostUri)?.findAll { Proxy proxy -> proxy.type() != Proxy.Type.DIRECT }
        this.proxies = proxyList ?: null

        this.sslProvider = settings.sslProvider
        this.sslSessionTimeout = settings.sslSessionTimeout
        this.sslSessionCacheSize = settings.sslSessionCacheSize
        this.sslTrustManagerFactory = settings.sslTrustManagerFactory
        this.sslContext = SslContextBuilder.forClient()
                .sslProvider(sslProvider)
                .sessionCacheSize(sslSessionCacheSize)
                .sessionTimeout(sslSessionTimeout)
                .trustManager(sslTrustManagerFactory)
                .build()

        this.allowBlockingOperations = settings.allowBlockingOperations
        this.interceptors.addAll(
                ConfigurationUtils.findServices( settings.interceptors, RequestInterceptor )
        )
        this.hosts = Observable.merge(socketAddresses.collect() { SocketAddress address -> Observable.just(new Host(address))})
        this.username = settings.username
        this.password = settings.password
        this.charset = Charset.forName( settings.charset )
        this.queryType = (settings.query.type == BSON_QUERY_TYPE) ? BsonRxRestQuery : SimpleRxRestQuery
        this.readTimeout = settings.readTimeout
        this.loggerLevel = settings.log.level
        this.loggerName = settings.log.name
        this.defaultUpdateMethod = settings.defaultUpdateMethod
        this.connectionProviderFactory = defaultConnectionSource.source
        this.codecRegistry = mappingContext.codecRegistry

        def clientConfiguration = new DefaultConfiguration()
        clientConfiguration.setReadTimeout(readTimeout)
        clientConfiguration.setEncoding(charset.toString())
        clientConfiguration.setSslProvider(sslProvider)
        clientConfiguration.setSslSessionCacheSize(sslSessionTimeout)
        clientConfiguration.setSslSessionCacheSize(sslSessionCacheSize)
        clientConfiguration.setSslTrustManagerFactory(sslTrustManagerFactory)

        this.rxHttpClientBuilder = new RxHttpClientBuilder(connectionProviderFactory, clientConfiguration)

        this.orderParameter = settings.parameters.order
        this.offsetParameter = settings.parameters.offset
        this.sortParameter = settings.parameters.sort
        this.maxParameter = settings.parameters.max
        this.queryParameter = settings.parameters.query
        this.expandParameter = settings.parameters.expand
        this.defaultParameterNames = new HashSet<>()
        defaultParameterNames.add(sortParameter)
        defaultParameterNames.add(maxParameter)
        defaultParameterNames.add(offsetParameter)
        defaultParameterNames.add(orderParameter)
        defaultParameterNames.add(queryParameter)
        defaultParameterNames.add(expandParameter)

        if(!(connectionSources instanceof SingletonConnectionSources)) {
            for(ConnectionSource<ConnectionProviderFactory, RestConnectionSourceSettings> connectionSource in connectionSources) {
                if(connectionSource.name == ConnectionSource.DEFAULT) continue

                SingletonConnectionSources<ConnectionProviderFactory, RestConnectionSourceSettings> singletonConnectionSources = new SingletonConnectionSources<>(connectionSource, connectionSources.baseConfiguration)
                RxRestDatastoreClient delegatingClient = createChildClient(singletonConnectionSources)

                datastoreClients.put(connectionSource.name, (RxDatastoreClient)delegatingClient)
            }
        }
        initialize(mappingContext)
    }

    protected RxRestDatastoreClient createChildClient(SingletonConnectionSources<ConnectionProviderFactory, RestConnectionSourceSettings> singletonConnectionSources) {
        new RxRestDatastoreClient(singletonConnectionSources, getMappingContext()) {
            @Override
            protected void initialize(RestClientMappingContext mc) {
                // no-op
            }
        }
    }

    RxRestDatastoreClient(ConnectionSources<ConnectionProviderFactory, RestConnectionSourceSettings> connectionSources, Class... classes) {
        this(connectionSources, createMappingContext(connectionSources.baseConfiguration, classes))
    }
    RxRestDatastoreClient(List<String> hosts, PropertyResolver configuration, Class... classes) {
        this(createDefaultConnectionSourcesForHosts(hosts, configuration), classes)
    }
    RxRestDatastoreClient(List<String> hosts, Class... classes) {
        this(createDefaultConnectionSourcesForHosts(hosts, DatastoreUtils.createPropertyResolver(null)), classes)
    }
    RxRestDatastoreClient(String host, PropertyResolver configuration, Class... classes) {
        this(createDefaultConnectionSourcesForHosts([host], configuration), classes)
    }
    RxRestDatastoreClient(String host, Class... classes) {
        this(createDefaultConnectionSourcesForHosts([host], DatastoreUtils.createPropertyResolver(null)), classes)
    }
    RxRestDatastoreClient(PropertyResolver configuration, Class... classes) {
        this(ConnectionSourcesInitializer.create(new RestConnectionSourceFactory(), configuration), createMappingContext(configuration, classes))
    }
    RxRestDatastoreClient(Class... classes) {
        this(DatastoreUtils.createPropertyResolver(null), classes)
    }

    @Override
    RestClientMappingContext getMappingContext() {
        return (RestClientMappingContext)super.getMappingContext()
    }

    @Override
    Observable<Number> batchDelete(BatchOperation operation) {
        Map operationArguments = operation.arguments
        HttpClient httpClient = createHttpClient(operationArguments)
        List<Observable<HttpClientResponse>> observables = []

        for (PersistentEntity entity in operation.deletes.keySet()) {
            RestEndpointPersistentEntity restEndpointPersistentEntity = (RestEndpointPersistentEntity)entity
            UriTemplate uriTemplate = operationArguments.containsKey(ARGUMENT_URI) ? UriTemplate.fromTemplate(operationArguments.get(ARGUMENT_URI).toString()) : restEndpointPersistentEntity.uriTemplate
            EntityReflector entityReflector = entity.getReflector()

            Map<Serializable, BatchOperation.EntityOperation> entityOperationMap = operation.deletes.get(entity)
            for (Serializable id in entityOperationMap.keySet()) {
                def object = entityOperationMap.get(id).object
                String uri = expandUri(uriTemplate, entityReflector, object)

                HttpClientRequest requestObservable = httpClient
                        .createDelete(uri)

                InterceptorContext context = new InterceptorContext(restEndpointPersistentEntity, (RxEntity)object)
                Observable<HttpClientResponse> deleteObservable = prepareRequest( requestObservable, context )
                def interceptorArgument = operationArguments.interceptor
                if(interceptorArgument instanceof RequestInterceptor) {
                    deleteObservable = ((RequestInterceptor)interceptorArgument).intercept(deleteObservable, context)
                }
                observables.add deleteObservable
            }
        }
        if (observables.isEmpty()) {
            return Observable.just((Number) 0L)
        } else {
            return (Observable<Number>) Observable.concatEager(observables)
                    .reduce(0, { Integer count, HttpClientResponse response ->

                def status = response.getStatus()
                if (status == HttpResponseStatus.NO_CONTENT || status == HttpResponseStatus.OK) {
                    count++
                }
                else {
                    throw new HttpClientException("Server returned error response: $status, reason: ${status.reasonPhrase()} for host ${response.hostHeader}")
                }
                return (Number) count
            })
        }
    }

    HttpClient<ByteBuf, ByteBuf> createHttpClient(Map<String,Object> arguments) {
        HttpClient httpClient = HttpClient.newClient(connectionProviderFactory, hosts)
        if(arguments?.containsKey(ARGUMENT_READ_TIMEOUT)) {
            httpClient = httpClient.readTimeOut(arguments.get(ARGUMENT_READ_TIMEOUT) as Integer, TimeUnit.MILLISECONDS)
        }
        else if(readTimeout > -1) {
            httpClient = httpClient.readTimeOut(readTimeout, TimeUnit.MILLISECONDS)
        }

        if(arguments?.containsKey(ARGUMENT_LOGGER_LEVEL)) {
            httpClient = httpClient.enableWireLogging(loggerName, arguments.get(ARGUMENT_LOGGER_LEVEL) as LogLevel)
        }
        else if(loggerLevel != null) {
            httpClient = httpClient.enableWireLogging(loggerName, loggerLevel)
        }

        httpClient = proxies != null ? configureProxy(httpClient) : httpClient

        if(isSecure) {
            if(sslTrustManagerFactory == InsecureTrustManagerFactory.INSTANCE) {
                return httpClient.unsafeSecure()
            }
            else {
                return httpClient.secure( { ByteBufAllocator allocator ->
                    return sslContext.newEngine(allocator)
                } as Func1<ByteBufAllocator, SSLEngine>)
            }
        }
        else {
            return httpClient
        }
    }


    @Override
    Observable<Number> batchWrite(BatchOperation operation) {
        Map operationArguments = operation.arguments
        HttpClient httpClient = createHttpClient(operationArguments)
        List<Observable> observables = []

        for(PersistentEntity entity in operation.inserts.keySet()) {
            RestEndpointPersistentEntity restEndpointPersistentEntity = (RestEndpointPersistentEntity)entity
            UriTemplate uriTemplate = operationArguments.containsKey(ARGUMENT_URI) ? UriTemplate.fromTemplate(operationArguments.get(ARGUMENT_URI).toString()) : restEndpointPersistentEntity.uriTemplate
            EntityReflector entityReflector = entity.getReflector()
            Map<Serializable, BatchOperation.EntityOperation> entityOperationMap = operation.inserts.get(entity)
            Class type = entity.getJavaClass()
            Codec codec = getCodecRegistry().get(type)
            String contentType = ((RestEndpointPersistentEntity) entity).getMapping().getMappedForm().contentType

            for(BatchOperation.EntityOperation entityOp in entityOperationMap.values()) {
                def object = entityOp.object
                String uri = expandUri(uriTemplate, entityReflector, object)
                Observable postObservable = httpClient.createPost(uri)
                postObservable = postObservable.setHeader( HttpHeaderNames.CONTENT_TYPE, contentType )
                                               .setHeader( HttpHeaderNames.ACCEPT, contentType)

                InterceptorContext context = new InterceptorContext(restEndpointPersistentEntity, (RxEntity)object)

                postObservable = prepareRequest( postObservable, context )

                def interceptorArgument = operationArguments.interceptor
                if(interceptorArgument instanceof RequestInterceptor) {
                    postObservable = ((RequestInterceptor)interceptorArgument).intercept(postObservable, context)
                }
                if(postObservable instanceof HttpClientRequest) {
                    postObservable = ((HttpClientRequest)postObservable).writeContent(
                            createContentWriteObservable(restEndpointPersistentEntity, codec, entityOp)
                    )
                }
                postObservable = postObservable.map { HttpClientResponse response ->
                    return new ResponseAndEntity(uri, response, entity, object, codec)
                }
                observables.add(postObservable)
            }
        }

        for(PersistentEntity entity in operation.updates.keySet()) {
            RestEndpointPersistentEntity restEndpointPersistentEntity = (RestEndpointPersistentEntity)entity
            Map<Serializable, BatchOperation.EntityOperation> entityOperationMap = operation.updates.get(entity)
            Class type = entity.getJavaClass()
            Codec codec = getCodecRegistry().get(type)
            UriTemplate uriTemplate = operationArguments.containsKey(ARGUMENT_URI) ? UriTemplate.fromTemplate(operationArguments.get(ARGUMENT_URI).toString()) : restEndpointPersistentEntity.uriTemplate
            String contentType = ((RestEndpointPersistentEntity) entity).getMapping().getMappedForm().contentType
            EntityReflector entityReflector = entity.getReflector()

            for(Serializable id in entityOperationMap.keySet()) {
                BatchOperation.EntityOperation entityOp = entityOperationMap.get(id)
                def object = entityOp.object
                String uri = expandUri(uriTemplate, entityReflector, object)

                HttpClientRequest requestObservable
                def methodArgument = operationArguments.method
                HttpMethod updateMethod =  methodArgument instanceof HttpMethod ? (HttpMethod)methodArgument : this.defaultUpdateMethod
                switch(updateMethod) {
                    case HttpMethod.PATCH:
                        requestObservable = httpClient.createPatch(uri)
                        break
                    case HttpMethod.POST:
                        requestObservable = httpClient.createPatch(uri)
                        break
                    default:
                        requestObservable = httpClient.createPut(uri)
                }
                requestObservable = requestObservable.setHeader( HttpHeaderNames.CONTENT_TYPE, contentType )
                                                     .setHeader( HttpHeaderNames.ACCEPT, contentType )

                InterceptorContext context = new InterceptorContext(restEndpointPersistentEntity, (RxEntity)object)

                Observable<HttpClientResponse> preparedObservable = prepareRequest(requestObservable, context)
                Observable finalObservable = preparedObservable

                def interceptorArgument = operationArguments.interceptor
                if(interceptorArgument instanceof RequestInterceptor) {
                    finalObservable = ((RequestInterceptor)interceptorArgument).intercept(preparedObservable, context)
                }

                if(finalObservable instanceof HttpClientRequest) {
                    finalObservable = ((HttpClientRequest)finalObservable).writeContent(
                            createContentWriteObservable(restEndpointPersistentEntity, codec, entityOp)
                    )
                }
                finalObservable = finalObservable.map { HttpClientResponse response ->
                    return new ResponseAndEntity(uri, response, entity, object, codec)
                }
                observables.add(finalObservable)
            }
        }

        if(observables.isEmpty()) {
            return Observable.just((Number)0L)
        }
        else {
            return (Observable<Number>)Observable.concatEager(observables)
                .switchMap { ResponseAndEntity responseAndEntity ->
                    HttpClientResponse response = responseAndEntity.response
                    HttpResponseStatus status = response.status
                    String responseContentType = response.getHeader(HttpHeaderNames.CONTENT_TYPE)
                    MediaType mediaType = responseContentType != null ? new MediaType(responseContentType) : null
                    responseAndEntity.mediaType = mediaType

                    if(status == HttpResponseStatus.CREATED ) {
                        if(MediaType.JSON == mediaType) {
                            return Observable.combineLatest( Observable.just(responseAndEntity), response.content.toList(), { ResponseAndEntity res, List<ByteBuf> content ->
                                res.content = content
                                return res
                            } as Func2<ResponseAndEntity, Object, ResponseAndEntity>)
                        }
                        else {
                            return Observable.just(responseAndEntity)
                        }
                    }
                    else if(status == HttpResponseStatus.OK) {
                        return Observable.just(responseAndEntity)
                    }
                    else if(status == HttpResponseStatus.UNPROCESSABLE_ENTITY ) {
                        if(MediaType.VND_ERROR == mediaType) {
                            return Observable.combineLatest( Observable.just(responseAndEntity), response.content.toList(), { ResponseAndEntity res, List<ByteBuf> content ->
                                res.content = content
                                return res
                            } as Func2<ResponseAndEntity, Object, ResponseAndEntity>)
                        }
                        else {
                            return Observable.just(responseAndEntity)
                        }
                    }
                    else {
                        throw new HttpClientException("Server returned error response: $status, reason: ${status.reasonPhrase()}")
                    }
                }
                .reduce(0L, { Long count, ResponseAndEntity responseAndContent ->
                    HttpClientResponse response = responseAndContent.response
                    List<ByteBuf> content = responseAndContent.content
                    HttpResponseStatus status = response.status
                    if(status == HttpResponseStatus.CREATED || status == HttpResponseStatus.OK) {
                        count++
                        if(content != null) {
                            ByteBuf byteBuf = Unpooled.wrappedBuffer(content as ByteBuf[])

                            def reader = new InputStreamReader(new ByteBufInputStream(byteBuf))
                            Codec codec = responseAndContent.codec
                            try {
                                Object decoded = codec.decode(new JsonReader(reader), BsonPersistentEntityCodec.DEFAULT_DECODER_CONTEXT)

                                EntityReflector reflector = responseAndContent.entity.reflector
                                def identifier = reflector.getIdentifier(decoded)
                                reflector.setIdentifier(responseAndContent.object, identifier)
                            }
                            catch(Throwable e) {
                                log.error "Error querying [$responseAndContent.entity.name] object for URI [$responseAndContent.uri]", e
                                throw new HttpClientException("Error decoding entity $responseAndContent.entity.name from response: $e.message", e)
                            }
                            finally {
                                byteBuf.release()
                                reader.close()
                            }

                        }
                    }
                else if(status == HttpResponseStatus.UNPROCESSABLE_ENTITY) {
                    Errors errors

                    if(MediaType.VND_ERROR == responseAndContent.mediaType && content != null) {
                        try {
                            ByteBuf byteBuf = Unpooled.wrappedBuffer(content as ByteBuf[])

                            def reader = new InputStreamReader(new ByteBufInputStream(byteBuf))
                            JsonReader jsonReader = new JsonReader(reader)
                            Codec<Errors> codec = mappingContext.get(Errors, codecRegistry)
                            def object = responseAndContent.object
                            if(codec instanceof ContextAwareCodec) {
                                ((ContextAwareCodec)codec).setContext(object)
                            }
                            errors = codec.decode(jsonReader, BsonPersistentEntityCodec.DEFAULT_DECODER_CONTEXT)
                            ((GormValidateable)object).setErrors(errors)
                        } catch (Throwable e) {
                            throw new HttpClientException("Error decoding validation errors from response: $e.message", e )
                        }

                    }
                    else {
                        errors = new ValidationErrors(responseAndContent.object, responseAndContent.entity.name)
                    }
                    throw new ValidationException("Validation error occured saving entity", errors)
                }
                return (Number)count
            })
        }
    }

    @Override
    void doClose() {
        // no-op
    }

    @Override
    Serializable generateIdentifier(PersistentEntity entity, Object instance, EntityReflector reflector) {
        // the identifier cannot be known since it will be assigned by the backend REST server, so use the hash code
        // for internal processing purposes
        return Integer.valueOf(instance.hashCode())
    }

    @Override
    Query createEntityQuery(PersistentEntity entity, QueryState queryState) {
        return createEntityQuery(entity, queryState, [:])
    }

    @Override
    Query createEntityQuery(PersistentEntity entity, QueryState queryState, Map arguments) {
        def queryTypeArg = arguments.get(ARGUMENT_QUERY_TYPE)
        if(queryTypeArg) {
            if(BSON_QUERY_TYPE.equals(queryTypeArg)) {
                return new BsonRxRestQuery(this, entity, queryState)
            }
            else {
                return queryType.newInstance(this, entity, queryState)
            }
        }
        else {
            return queryType.newInstance(this, entity, queryState)
        }
    }

    final Query createQuery(Class type, UriTemplate uriTemplate, QueryState queryState, Map arguments = Collections.emptyMap()) {

        def entity = mappingContext.getPersistentEntity(type.name)
        if(entity == null) {
            throw new IllegalArgumentException("Type [$type.name] is not a persistent type")
        }
        def queryTypeArg = arguments.get(ARGUMENT_QUERY_TYPE)
        if(queryTypeArg) {
            if(BSON_QUERY_TYPE.equals(queryTypeArg)) {
                return new BsonRxRestQuery(this, entity, uriTemplate, queryState)
            }
            else {
                return queryType.newInstance(this, entity, uriTemplate, queryState)
            }
        }
        else {
            return queryType.newInstance(this, entity, uriTemplate, queryState)
        }

    }

    @Override
    RxHttpClientBuilder getNativeInterface() {
        return rxHttpClientBuilder
    }

    protected Observable<ByteBuf> createContentWriteObservable(RestEndpointPersistentEntity restEndpointPersistentEntity, Codec codec, BatchOperation.EntityOperation entityOp) {
        Observable.create({ Subscriber<ByteBuf> subscriber ->
            ByteBuf byteBuf = Unpooled.buffer()
            try {
                def writer = new OutputStreamWriter(new ByteBufOutputStream(byteBuf), restEndpointPersistentEntity.charset)
                def jsonWriter = new JsonWriter(writer)
                codec.encode(jsonWriter, entityOp.object, BsonPersistentEntityCodec.DEFAULT_ENCODER_CONTEXT)

                subscriber.onNext(byteBuf)
            } catch (Throwable e) {
                log.error "Error encoding object [$entityOp.object] to JSON: $e.message", e
                subscriber.onError(e)
            }
            finally {
                subscriber.onCompleted()
            }

        } as Observable.OnSubscribe<ByteBuf>)
    }

    protected String expandUri(UriTemplate uriTemplate, EntityReflector entityReflector, object) {
        Map<String, Object> vars = [:]
        for (var in uriTemplate.variables) {
            def value = entityReflector.getProperty(object, var)
            if(value instanceof RxEntity) {
                def id = getMappingContext().getProxyHandler().getIdentifier(value)
                if(id != null) {
                    vars.put(var, value)
                }
            }
            else if (value != null) {
                vars.put(var, value)
            }
        }

        String uri = uriTemplate.expand(vars)
        return uri
    }

    protected void initialize(RestClientMappingContext mappingContext) {
        for (entity in mappingContext.persistentEntities) {
            RxGormEnhancer.registerEntity(entity, this)
        }

        initDefaultConverters(mappingContext)
        initDefaultEventListeners(eventPublisher)
    }


    protected void initDefaultConverters(RestClientMappingContext mappingContext) {
        TimeZone UTC = TimeZone.getTimeZone("UTC");
        mappingContext.converterRegistry.addConverter(new Converter<String, Date>() {
            @Override
            Date convert(String source) {
                def format = new SimpleDateFormat(JsonWriter.ISO_8601)
                format.setTimeZone(UTC)
                return format.parse(source)
            }
        })
    }

    protected static List<SocketAddress> createServerSocketAddresses(PropertyResolver configuration) {
        List hosts = configuration.getProperty(SETTING_HOSTS, List, Collections.emptyList())

        if(hosts.isEmpty()) {

            String hostString = configuration.getProperty(SETTING_HOST, String.class, "http://localhost:8080")

            try {
                URI uri = new URI(hostString)
                int defaultPort = uri.scheme == 'https' ? 443 : 80
                return [new InetSocketAddress(uri.host, uri.port > -1 ? uri.port : defaultPort) ] as List<SocketAddress>
            } catch (URISyntaxException e) {
                throw new DatastoreConfigurationException("Invalid host configuration specified: $hostString")
            }
        }
        else {
            return hosts.collect() { Object it ->
                try {
                    URI uri = new URI(it.toString())
                    int defaultPort = uri.scheme == 'https' ? 443 : 80
                    return new InetSocketAddress(uri.host, uri.port > -1 ? uri.port : defaultPort)
                } catch (URISyntaxException e) {
                    throw new DatastoreConfigurationException("Invalid host configuration specified: $it")
                }
            } as List<SocketAddress>
        }

    }

    Observable<HttpClientResponse> prepareRequest(HttpClientRequest<ByteBuf, ByteBuf> httpClientRequest, InterceptorContext context) {
        Observable<HttpClientResponse> finalRequest = httpClientRequest
        if (username != null && password != null) {
            String usernameAndPassword = "$username:$password"
            def encoded = Base64.encode(Unpooled.wrappedBuffer(usernameAndPassword.bytes)).toString(charset)
            finalRequest = httpClientRequest.addHeader HttpHeaderNames.AUTHORIZATION, "Basic $encoded".toString()
        }

        for(RequestInterceptor i in interceptors) {
            finalRequest = i.intercept(finalRequest, context)
        }
        for(RequestInterceptor i in context.entity.interceptors) {
            finalRequest = i.intercept(finalRequest, context)
        }
        return finalRequest
    }

    @Override
    boolean isAllowBlockingOperations() {
        return this.allowBlockingOperations
    }

    @Override
    RxRestGormStaticApi createStaticApi(PersistentEntity entity, String connectionSourceName) {
        return new RxRestGormStaticApi(entity, getDatastoreClient(connectionSourceName))
    }

    @Override
    RxRestGormInstanceApi createInstanceApi(PersistentEntity entity, String connectionSourceName) {
        return new RxRestGormInstanceApi(entity, getDatastoreClient(connectionSourceName))
    }

    @Override
    def <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {
        getMappingContext().get(clazz, codecRegistry)
    }

    protected static InMemoryConnectionSources<ConnectionProviderFactory, RestConnectionSourceSettings> createDefaultConnectionSourcesForHosts(List<String> hosts, PropertyResolver configuration) {
        RestConnectionSourceSettingsBuilder builder = new RestConnectionSourceSettingsBuilder(configuration)
        RestConnectionSourceSettings settings = builder.build()
        settings.hosts(hosts)

        RestConnectionSourceFactory factory = new RestConnectionSourceFactory()
        ConnectionSource<ConnectionProviderFactory, RestConnectionSourceSettings> connectionSource = factory.create(ConnectionSource.DEFAULT, settings)
        return new InMemoryConnectionSources<ConnectionProviderFactory, RestConnectionSourceSettings>(
                connectionSource,
                factory,
                configuration
        )
    }
    protected static RestClientMappingContext createMappingContext(PropertyResolver configuration, Class... classes) {
        RestClientMappingContext mappingContext = new RestClientMappingContext(configuration, classes)
        mappingContext.setValidatorRegistry(new DefaultValidatorRegistry(mappingContext, configuration))
        return mappingContext
    }

    /**
     * Configures any {@link Proxy} instances for the client
     *
     * @param httpClient The client
     * @return The configured client
     */
    protected HttpClient<ByteBuf, ByteBuf> configureProxy(HttpClient<ByteBuf, ByteBuf> httpClient) {
        if(proxies != null) {
            for(Proxy proxy in proxies) {
                Proxy.Type proxyType = proxy.type()
                if(proxyType == Proxy.Type.DIRECT) continue

                def type = proxyType.name().toLowerCase()
                String username = System.getProperty("${type}.proxyUser")
                String password = System.getProperty("${type}.proxyPassword")

                httpClient = httpClient.addChannelHandlerLast("${type}.proxy", {
                    if(username && password) {
                        switch(type) {
                            case Proxy.Type.SOCKS:
                                return new Socks5ProxyHandler(proxy.address(), username, password)
                                break
                            default:
                                return new HttpProxyHandler(proxy.address(), username, password)
                                break

                        }
                    }
                    else {
                        switch(type) {
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
        }
        return httpClient
    }

    private static class ResponseAndEntity {
        final String uri
        final HttpClientResponse response
        final PersistentEntity entity
        final Object object
        final Codec codec

        List<ByteBuf> content
        MediaType mediaType

        ResponseAndEntity(String uri, HttpClientResponse response, PersistentEntity entity, Object object, Codec codec) {
            this.uri = uri
            this.response = response
            this.entity = entity
            this.object = object
            this.codec = codec
        }
    }

}
