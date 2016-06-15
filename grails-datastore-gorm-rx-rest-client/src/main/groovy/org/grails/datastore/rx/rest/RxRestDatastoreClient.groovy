package org.grails.datastore.rx.rest

import groovy.transform.CompileStatic
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufHolder
import io.netty.buffer.ByteBufInputStream
import io.netty.buffer.Unpooled
import io.netty.handler.codec.base64.Base64
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpResponseStatus
import io.reactivex.netty.client.ConnectionProviderFactory
import io.reactivex.netty.client.Host
import io.reactivex.netty.client.pool.PoolConfig
import io.reactivex.netty.client.pool.SingleHostPoolingProviderFactory
import io.reactivex.netty.protocol.http.client.HttpClient
import io.reactivex.netty.protocol.http.client.HttpClientRequest
import io.reactivex.netty.protocol.http.client.HttpClientResponse
import org.bson.codecs.Codec
import org.bson.codecs.configuration.CodecRegistry
import org.grails.datastore.bson.codecs.BsonPersistentEntityCodec
import org.grails.datastore.bson.json.JsonReader
import org.grails.datastore.bson.json.JsonWriter
import org.grails.datastore.gorm.events.ConfigurableApplicationEventPublisher
import org.grails.datastore.mapping.core.DatastoreUtils
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.query.Query
import org.grails.datastore.mapping.reflect.EntityReflector
import org.grails.datastore.rx.AbstractRxDatastoreClient
import org.grails.datastore.rx.batch.BatchOperation
import org.grails.datastore.rx.query.QueryState
import org.grails.datastore.rx.rest.api.RxRestGormStaticApi
import org.grails.datastore.rx.rest.config.RestClientMappingContext
import org.grails.datastore.rx.rest.paths.DefaultResourcePathResolver
import org.grails.datastore.rx.rest.paths.ResourcePathResolver
import org.grails.datastore.rx.rest.query.SimpleRxRestQuery
import org.grails.gorm.rx.api.RxGormEnhancer
import org.grails.gorm.rx.api.RxGormStaticApi
import org.grails.gorm.rx.events.AutoTimestampEventListener
import org.grails.gorm.rx.events.DomainEventListener
import org.springframework.core.convert.converter.Converter
import org.springframework.core.env.PropertyResolver
import rx.Observable

import java.nio.charset.Charset
import java.text.SimpleDateFormat

/**
 * An RxGORM implementation that backs onto a backend REST server
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
class RxRestDatastoreClient extends AbstractRxDatastoreClient<ConnectionProviderFactory> {

    public static final String HOST = "grails.gorm.rest.host"
    public static final String PORT = "grails.gorm.rest.port"
    public static final String CHARSET = "grails.gorm.rest.charset"
    public static final String OPTIONS = "grails.gorm.rest.options"
    public static final String USERNAME = "grails.gorm.rest.username"
    public static final String PASSWORD = "grails.gorm.rest.password"


    final ConnectionProviderFactory connectionProviderFactory
    final Observable<Host> defaultClientHost
    final ResourcePathResolver pathResolver
    final CodecRegistry codecRegistry
    final String username
    final String password
    final Charset charset

    RxRestDatastoreClient(SocketAddress serverAddress, PropertyResolver configuration, RestClientMappingContext mappingContext) {
        super(mappingContext)

        this.defaultClientHost = Observable.just(new Host(serverAddress))
        this.pathResolver = new DefaultResourcePathResolver(mappingContext)
        this.username = configuration.getProperty(USERNAME, String, null)
        this.password = configuration.getProperty(PASSWORD, String, null)
        this.charset = Charset.forName( configuration.getProperty(CHARSET, "UTF-8"))
        def pool = new PoolConfig()
        // TODO: populate pool configuration
        connectionProviderFactory = SingleHostPoolingProviderFactory.create(pool)
        this.codecRegistry = mappingContext.codecRegistry

        initialize(mappingContext)
    }

    RxRestDatastoreClient(SocketAddress serverAddress, RestClientMappingContext mappingContext) {
        this(serverAddress, DatastoreUtils.createPropertyResolver(null), mappingContext)
    }

    RxRestDatastoreClient(SocketAddress serverAddress, PropertyResolver configuration, Class... classes) {
        this(serverAddress, configuration, createMappingContext(configuration, classes))
    }

    RxRestDatastoreClient(SocketAddress serverAddress, Class... classes) {
        this(serverAddress, createMappingContext(DatastoreUtils.createPropertyResolver(null), classes))
    }

    RxRestDatastoreClient(PropertyResolver configuration, RestClientMappingContext mappingContext) {
        this(createServerSocketAddress(configuration), configuration, mappingContext)
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

    protected void initialize(RestClientMappingContext mappingContext) {
        for (entity in mappingContext.persistentEntities) {
            RxGormEnhancer.registerEntity(entity, this)
        }

        initDefaultConverters(mappingContext)
        initDefaultEventListeners(eventPublisher)
    }

    void initDefaultConverters(RestClientMappingContext mappingContext) {
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

    protected void initDefaultEventListeners(ConfigurableApplicationEventPublisher configurableApplicationEventPublisher) {
        configurableApplicationEventPublisher.addApplicationListener(new AutoTimestampEventListener(this))
        configurableApplicationEventPublisher.addApplicationListener(new DomainEventListener(this))
    }


    protected static InetSocketAddress createServerSocketAddress(PropertyResolver configuration) {
        new InetSocketAddress(configuration.getProperty(HOST, String.class, "localhost"), configuration.getProperty(PORT, Integer.class, 8080))
    }

    @Override
    def <T> Observable get(Class<T> type, Serializable id, QueryState queryState) {

        PersistentEntity entity = getMappingContext().getPersistentEntity(type.name)
        if (entity == null) {
            throw new IllegalArgumentException("Type [$type.name] is not a persistent type")
        }

        def loadedEntity = queryState.getLoadedEntity(type, id)
        if(loadedEntity != null) {
            return Observable.just(loadedEntity)
        }

        HttpClient httpClient = createHttpClient()
        String uri = pathResolver.getPath(entity.getJavaClass(), id)


        HttpClientRequest httpClientRequest = httpClient
                            .createGet(uri)

        prepareRequest(httpClientRequest)

        httpClientRequest
                .switchMap { HttpClientResponse response ->
            response.getContent()
        }.map { Object object ->
            ByteBuf byteBuf
            if (object instanceof ByteBuf) {
                byteBuf = (ByteBuf) object
            } else if (object instanceof ByteBufHolder) {
                byteBuf = ((ByteBufHolder) object).content()
            } else {
                throw new IllegalStateException("Received invalid response object: $object")
            }

            def reader = new InputStreamReader(new ByteBufInputStream(byteBuf))
            Codec codec = getMappingContext().get(type, codecRegistry)
            try {
                def decoded = codec.decode(new JsonReader(reader), BsonPersistentEntityCodec.DEFAULT_DECODER_CONTEXT)
                return decoded
            } finally {
                byteBuf.release()
                reader.close()
            }
        }
    }

    @Override
    Observable<Number> batchDelete(BatchOperation operation) {
        HttpClient httpClient = createHttpClient()
        List<HttpClientRequest> observables = []

        for (PersistentEntity entity in operation.deletes.keySet()) {
            Map<Serializable, BatchOperation.EntityOperation> entityOperationMap = operation.deletes.get(entity)
            for (Serializable id in entityOperationMap.keySet()) {

                String uri = pathResolver.getPath(entity.getJavaClass(), id)

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
                return (Number) count
            })
        }
    }

    protected void prepareRequest(HttpClientRequest<ByteBuf, ByteBuf> httpClientRequest) {
        if (username != null && password != null) {
            String usernameAndPassword = "$username:$password"
            def encoded = Base64.encode(Unpooled.wrappedBuffer(usernameAndPassword.bytes)).toString(charset)
            httpClientRequest.addHeader HttpHeaderNames.AUTHORIZATION, "Basic $encoded".toString()
        }
    }

    HttpClient<ByteBuf, ByteBuf> createHttpClient() {
        return HttpClient.newClient(connectionProviderFactory, defaultClientHost)
    }

    @Override
    Observable<Number> batchWrite(BatchOperation operation) {
        return null
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
        return new SimpleRxRestQuery(this, entity, queryState)
    }

    @Override
    ConnectionProviderFactory getNativeInterface() {
        return connectionProviderFactory
    }

    protected static RestClientMappingContext createMappingContext(PropertyResolver configuration, Class... classes) {
        return new RestClientMappingContext(classes)
    }

    @Override
    RxGormStaticApi createStaticApi(PersistentEntity entity) {
        return new RxRestGormStaticApi(entity, this)
    }
}
