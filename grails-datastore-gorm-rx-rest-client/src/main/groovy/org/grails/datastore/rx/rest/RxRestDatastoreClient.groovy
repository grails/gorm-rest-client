package org.grails.datastore.rx.rest

import com.damnhandy.uri.template.UriTemplate
import grails.gorm.rx.RxEntity
import grails.gorm.rx.rest.interceptor.RequestInterceptor
import grails.http.HttpMethod
import grails.http.MediaType
import grails.http.client.RxHttpClientBuilder
import grails.http.client.cfg.DefaultConfiguration
import grails.http.client.exceptions.HttpClientException
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufHolder
import io.netty.buffer.ByteBufInputStream
import io.netty.buffer.ByteBufOutputStream
import io.netty.buffer.Unpooled
import io.netty.handler.codec.base64.Base64
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.logging.LogLevel
import io.reactivex.netty.client.ConnectionProviderFactory
import io.reactivex.netty.client.Host
import io.reactivex.netty.client.loadbalancer.LoadBalancerFactory
import io.reactivex.netty.client.loadbalancer.LoadBalancingStrategy
import io.reactivex.netty.client.pool.PoolConfig
import io.reactivex.netty.client.pool.SingleHostPoolingProviderFactory
import io.reactivex.netty.protocol.http.client.HttpClient
import io.reactivex.netty.protocol.http.client.HttpClientRequest
import io.reactivex.netty.protocol.http.client.HttpClientResponse
import io.reactivex.netty.protocol.http.client.loadbalancer.EWMABasedP2CStrategy
import org.bson.codecs.Codec
import org.bson.codecs.configuration.CodecRegistry
import org.grails.datastore.bson.codecs.BsonPersistentEntityCodec
import org.grails.datastore.bson.json.JsonReader
import org.grails.datastore.bson.json.JsonWriter
import org.grails.datastore.gorm.GormValidateable
import org.grails.datastore.gorm.validation.constraints.registry.DefaultValidatorRegistry
import org.grails.datastore.mapping.config.ConfigurationUtils
import org.grails.datastore.mapping.core.DatastoreUtils
import org.grails.datastore.mapping.model.DatastoreConfigurationException
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.query.Query
import org.grails.datastore.mapping.reflect.EntityReflector
import org.grails.datastore.mapping.validation.ValidationErrors
import org.grails.datastore.mapping.validation.ValidationException
import org.grails.datastore.rx.AbstractRxDatastoreClient
import org.grails.datastore.rx.batch.BatchOperation
import org.grails.datastore.rx.bson.CodecsRxDatastoreClient
import org.grails.datastore.rx.query.QueryState
import org.grails.datastore.rx.rest.api.RxRestGormStaticApi
import org.grails.datastore.rx.rest.codecs.ContextAwareCodec
import org.grails.datastore.rx.rest.config.PoolConfigBuilder
import org.grails.datastore.rx.rest.config.RestClientMappingContext
import org.grails.datastore.rx.rest.config.Settings
import org.grails.datastore.rx.rest.query.BsonRxRestQuery
import org.grails.datastore.rx.rest.query.SimpleRxRestQuery
import org.grails.gorm.rx.api.RxGormEnhancer
import org.grails.gorm.rx.api.RxGormStaticApi
import org.springframework.core.convert.converter.Converter
import org.springframework.core.env.PropertyResolver
import org.springframework.validation.Errors
import rx.Observable
import rx.Subscriber
import rx.functions.Func2

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
class RxRestDatastoreClient extends AbstractRxDatastoreClient<RxHttpClientBuilder> implements CodecsRxDatastoreClient<RxHttpClientBuilder>, Settings {

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
    final LogLevel logLevel
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

    protected final boolean allowBlockingOperations


    RxRestDatastoreClient(List<SocketAddress> serverAddress, PropertyResolver configuration, RestClientMappingContext mappingContext) {
        super(mappingContext)

        if(serverAddress.isEmpty()) {
            throw new IllegalArgumentException("At least 1 server address is required")
        }

        this.allowBlockingOperations = configuration.getProperty(SETTING_ALLOW_BLOCKING, Boolean, true)
        this.hosts = Observable.merge(serverAddress.collect() { SocketAddress address -> Observable.just(new Host(address))})
        this.username = configuration.getProperty(SETTING_USERNAME, String, null)
        this.password = configuration.getProperty(SETTING_PASSWORD, String, null)
        this.charset = Charset.forName( configuration.getProperty(SETTING_CHARSET, "UTF-8"))
        this.queryType = (configuration.getProperty(SETTING_QUERY_TYPE, String, "simple") == "bson") ? BsonRxRestQuery : SimpleRxRestQuery
        this.readTimeout = configuration.getProperty(SETTING_READ_TIMEOUT, Integer, -1)
        this.logLevel = configuration.getProperty(SETTING_LOG_LEVEL, LogLevel, null)
        this.defaultUpdateMethod = configuration.getProperty(SETTING_UPDATE_METHOD, HttpMethod, HttpMethod.PUT)

        if(serverAddress.size() == 1) {
            PoolConfigBuilder poolConfigBuilder = new PoolConfigBuilder(configuration)
            PoolConfig pool = poolConfigBuilder.build()
            this.connectionProviderFactory = SingleHostPoolingProviderFactory.create(pool)
        }
        else {
            final LoadBalancingStrategy loadBalanceStrategy = configuration.getProperty(SETTING_LOAD_BALANCE_STRATEGY, LoadBalancingStrategy, new EWMABasedP2CStrategy())
            this.connectionProviderFactory = LoadBalancerFactory.create(loadBalanceStrategy)
        }
        this.codecRegistry = mappingContext.codecRegistry
        def clientConfiguration = new DefaultConfiguration()
        clientConfiguration.setEncoding(charset.toString())
        this.rxHttpClientBuilder = new RxHttpClientBuilder(connectionProviderFactory, clientConfiguration)
        interceptors.addAll(
                ConfigurationUtils.findServices(configuration, SETTING_INTERCEPTORS, RequestInterceptor.class)
        )

        this.orderParameter = configuration.getProperty(SETTING_ORDER_PARAMETER, String, DEFAULT_ORDER_PARAMETER)
        this.offsetParameter = configuration.getProperty(SETTING_OFFSET_PARAMETER, String, DEFAULT_OFFSET_PARAMETER)
        this.sortParameter = configuration.getProperty(SETTING_SORT_PARAMETER, String, DEFAULT_SORT_PARAMETER)
        this.maxParameter = configuration.getProperty(SETTING_MAX_PARAMETER, String, DEFAULT_MAX_PARAMETER)
        this.queryParameter = configuration.getProperty(SETTING_QUERY_PARAMETER, String, DEFAULT_QUERY_PARAMETER)
        this.expandParameter = configuration.getProperty(SETTING_EXPAND_PARAMETER, String, DEFAULT_EXPAND_PARAMETER)
        this.defaultParameterNames = new HashSet<>()
        defaultParameterNames.add(sortParameter)
        defaultParameterNames.add(maxParameter)
        defaultParameterNames.add(offsetParameter)
        defaultParameterNames.add(orderParameter)
        defaultParameterNames.add(queryParameter)
        defaultParameterNames.add(expandParameter)

        initialize(mappingContext)
    }

    RxRestDatastoreClient(List<SocketAddress> socketAddresses, RestClientMappingContext mappingContext) {
        this(socketAddresses, DatastoreUtils.createPropertyResolver(null), mappingContext)
    }

    RxRestDatastoreClient(List<SocketAddress> socketAddresses, PropertyResolver configuration, Class... classes) {
        this(socketAddresses, configuration, createMappingContext(configuration, classes))
    }

    RxRestDatastoreClient(List<SocketAddress> socketAddresses, Class... classes) {
        this(socketAddresses, createMappingContext(DatastoreUtils.createPropertyResolver(null), classes))
    }

    RxRestDatastoreClient(SocketAddress serverAddress, RestClientMappingContext mappingContext) {
        this([serverAddress], DatastoreUtils.createPropertyResolver(null), mappingContext)
    }

    RxRestDatastoreClient(SocketAddress serverAddress, PropertyResolver configuration, Class... classes) {
        this([serverAddress], configuration, createMappingContext(configuration, classes))
    }

    RxRestDatastoreClient(SocketAddress serverAddress, Class... classes) {
        this(serverAddress, createMappingContext(DatastoreUtils.createPropertyResolver(null), classes))
    }

    RxRestDatastoreClient(PropertyResolver configuration, RestClientMappingContext mappingContext) {
        this(createServerSocketAddresses(configuration), configuration, mappingContext)
    }

    RxRestDatastoreClient(RestClientMappingContext mappingContext) {
        this(DatastoreUtils.createPropertyResolver(null), mappingContext)
    }

    RxRestDatastoreClient(PropertyResolver configuration, Class... classes) {
        this(configuration, createMappingContext(configuration, classes))
    }

    RxRestDatastoreClient(Class... classes) {
        this(createMappingContext(DatastoreUtils.createPropertyResolver(null), classes))
    }

    @Override
    RestClientMappingContext getMappingContext() {
        return (RestClientMappingContext)super.getMappingContext()
    }

    @Override
    Observable<Number> batchDelete(BatchOperation operation) {
        Map operationArguments = operation.arguments
        HttpClient httpClient = createHttpClient(operationArguments)
        List<HttpClientRequest> observables = []

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

                observables.add requestObservable
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

        if(arguments?.containsKey(ARGUMENT_LOG_LEVEL)) {
            httpClient = httpClient.enableWireLogging(arguments.get(ARGUMENT_LOG_LEVEL) as LogLevel)
        }
        else if(logLevel != null) {
            httpClient = httpClient.enableWireLogging(logLevel)
        }
        return httpClient

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
                postObservable = prepareRequest(restEndpointPersistentEntity, postObservable, object)

                def interceptorArgument = operationArguments.interceptor
                if(interceptorArgument instanceof RequestInterceptor) {
                    postObservable = ((RequestInterceptor)interceptorArgument).intercept(restEndpointPersistentEntity, (RxEntity)object, postObservable)
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
                Observable<HttpClientResponse> preparedObservable = prepareRequest(restEndpointPersistentEntity, requestObservable, object)
                Observable finalObservable = preparedObservable

                def interceptorArgument = operationArguments.interceptor
                if(interceptorArgument instanceof RequestInterceptor) {
                    finalObservable = ((RequestInterceptor)interceptorArgument).intercept(restEndpointPersistentEntity, (RxEntity)object, preparedObservable)
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
                            return Observable.combineLatest( Observable.just(responseAndEntity), response.content, { ResponseAndEntity res, Object content ->
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
                            return Observable.combineLatest( Observable.just(responseAndEntity), response.content, { ResponseAndEntity res, Object content ->
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
                    Object content = responseAndContent.content
                    HttpResponseStatus status = response.status
                    if(status == HttpResponseStatus.CREATED || status == HttpResponseStatus.OK) {
                        count++
                        if(content != null) {
                            ByteBuf byteBuf
                            if (content instanceof ByteBuf) {
                                byteBuf = (ByteBuf) content
                            } else if (content instanceof ByteBufHolder) {
                                byteBuf = ((ByteBufHolder) content).content()
                            } else {
                                throw new IllegalStateException("Received invalid response object: $content")
                            }

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
                            ByteBuf byteBuf
                            if (content instanceof ByteBuf) {
                                byteBuf = (ByteBuf) content
                            } else if (content instanceof ByteBufHolder) {
                                byteBuf = ((ByteBufHolder) content).content()
                            } else {
                                throw new IllegalStateException("Received invalid response object: $content")
                            }

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
        return queryType.newInstance(this, entity, queryState)
    }


    final Query createQuery(Class type, UriTemplate uriTemplate, QueryState queryState) {
        def entity = mappingContext.getPersistentEntity(type.name)
        if(entity == null) {
            throw new IllegalArgumentException("Type [$type.name] is not a persistent type")
        }

        return queryType.newInstance(this, entity, uriTemplate, queryState)
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
                return [ new InetSocketAddress(uri.host, uri.port > -1 ? uri.port : 80) ] as List<SocketAddress>
            } catch (URISyntaxException e) {
                throw new DatastoreConfigurationException("Invalid host configuration specified: $hostString")
            }
        }
        else {
            return hosts.collect() { Object it ->
                try {
                    URI uri = new URI(it.toString())
                    return new InetSocketAddress(uri.host, uri.port > -1 ? uri.port : 80)
                } catch (URISyntaxException e) {
                    throw new DatastoreConfigurationException("Invalid host configuration specified: $it")
                }
            } as List<SocketAddress>
        }

    }

    protected static RestClientMappingContext createMappingContext(PropertyResolver configuration, Class... classes) {
        RestClientMappingContext mappingContext = new RestClientMappingContext(configuration, classes)
        mappingContext.setValidatorRegistry(new DefaultValidatorRegistry(mappingContext, configuration))
        return mappingContext
    }

    Observable<HttpClientResponse> prepareRequest(RestEndpointPersistentEntity restEndpointPersistentEntity, HttpClientRequest<ByteBuf, ByteBuf> httpClientRequest, Object instance = null) {
        Observable<HttpClientResponse> finalRequest = httpClientRequest
        if (username != null && password != null) {
            String usernameAndPassword = "$username:$password"
            def encoded = Base64.encode(Unpooled.wrappedBuffer(usernameAndPassword.bytes)).toString(charset)
            finalRequest = httpClientRequest.addHeader HttpHeaderNames.AUTHORIZATION, "Basic $encoded".toString()
        }

        for(RequestInterceptor i in interceptors) {
            finalRequest = i.intercept(restEndpointPersistentEntity, (RxEntity)instance, finalRequest)
        }
        for(RequestInterceptor i in restEndpointPersistentEntity.interceptors) {
            finalRequest = i.intercept(restEndpointPersistentEntity, (RxEntity)instance, finalRequest)
        }
        return finalRequest
    }

    @Override
    boolean isAllowBlockingOperations() {
        return this.allowBlockingOperations
    }

    @Override
    RxGormStaticApi createStaticApi(PersistentEntity entity) {
        return new RxRestGormStaticApi(entity, this)
    }

    @Override
    def <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {
        getMappingContext().get(clazz, codecRegistry)
    }

    private static class ResponseAndEntity {
        final String uri
        final HttpClientResponse response
        final PersistentEntity entity
        final Object object
        final Codec codec

        Object content
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
