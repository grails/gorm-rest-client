package org.grails.datastore.rx.rest

import groovy.transform.CompileStatic
import io.netty.buffer.ByteBuf
import io.netty.handler.codec.http.HttpResponseStatus
import io.reactivex.netty.client.ConnectionProviderFactory
import io.reactivex.netty.client.Host
import io.reactivex.netty.client.pool.PoolConfig
import io.reactivex.netty.client.pool.SingleHostPoolingProviderFactory
import io.reactivex.netty.protocol.http.client.HttpClient
import io.reactivex.netty.protocol.http.client.HttpClientRequest
import io.reactivex.netty.protocol.http.client.HttpClientResponse
import org.bson.codecs.configuration.CodecRegistry
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

    public static final String SETTING_HOST = "grails.gorm.rest.host"
    public static final String SETTING_PORT = "grails.gorm.rest.port"
    public static final String SETTING_CHARSET = "grails.gorm.rest.charset"
    public static final String SETTING_POOL_OPTIONS = "grails.gorm.rest.pool.options"
    public static final String SETTING_USERNAME = "grails.gorm.rest.username"
    public static final String SETTING_PASSWORD = "grails.gorm.rest.password"
    public static final String SETTING_ORDER_PARAMETER = "grails.gorm.rest.parameters.order"
    public static final String SETTING_SORT_PARAMETER = "grails.gorm.rest.parameters.sort"
    public static final String SETTING_MAX_PARAMETER = "grails.gorm.rest.parameters.max"
    public static final String SETTING_OFFSET_PARAMETER = "grails.gorm.rest.parameters.offset"
    public static final String DEFAULT_ORDER_PARAMETER = "order"
    public static final String DEFAULT_OFFSET_PARAMETER = "offset"
    public static final String DEFAULT_SORT_PARAMETER = "sort"
    public static final String DEFAULT_MAX_PARAMETER = "max"


    final ConnectionProviderFactory connectionProviderFactory
    final Observable<Host> defaultClientHost
    final CodecRegistry codecRegistry
    final String username
    final String password
    final Charset charset
    final String orderParameter
    final String offsetParameter
    final String maxParameter
    final String sortParameter

    /**
     * The resolver used to resolve paths to resources
     */
    ResourcePathResolver pathResolver

    RxRestDatastoreClient(SocketAddress serverAddress, PropertyResolver configuration, RestClientMappingContext mappingContext) {
        super(mappingContext)

        this.defaultClientHost = Observable.just(new Host(serverAddress))
        this.pathResolver = new DefaultResourcePathResolver(mappingContext)
        this.username = configuration.getProperty(SETTING_USERNAME, String, null)
        this.password = configuration.getProperty(SETTING_PASSWORD, String, null)
        this.charset = Charset.forName( configuration.getProperty(SETTING_CHARSET, "UTF-8"))
        def pool = new PoolConfig()
        // TODO: populate pool configuration
        connectionProviderFactory = SingleHostPoolingProviderFactory.create(pool)
        this.codecRegistry = mappingContext.codecRegistry

        this.orderParameter = configuration.getProperty(SETTING_ORDER_PARAMETER, String, DEFAULT_ORDER_PARAMETER)
        this.offsetParameter = configuration.getProperty(SETTING_OFFSET_PARAMETER, String, DEFAULT_OFFSET_PARAMETER)
        this.sortParameter = configuration.getProperty(SETTING_SORT_PARAMETER, String, DEFAULT_SORT_PARAMETER)
        this.maxParameter = configuration.getProperty(SETTING_MAX_PARAMETER, String, DEFAULT_MAX_PARAMETER)
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

    protected void initDefaultEventListeners(ConfigurableApplicationEventPublisher configurableApplicationEventPublisher) {
        configurableApplicationEventPublisher.addApplicationListener(new AutoTimestampEventListener(this))
        configurableApplicationEventPublisher.addApplicationListener(new DomainEventListener(this))
    }


    protected static InetSocketAddress createServerSocketAddress(PropertyResolver configuration) {
        new InetSocketAddress(configuration.getProperty(SETTING_HOST, String.class, "localhost"), configuration.getProperty(SETTING_PORT, Integer.class, 8080))
    }

    protected static RestClientMappingContext createMappingContext(PropertyResolver configuration, Class... classes) {
        return new RestClientMappingContext(classes)
    }

    @Override
    RxGormStaticApi createStaticApi(PersistentEntity entity) {
        return new RxRestGormStaticApi(entity, this)
    }
}
