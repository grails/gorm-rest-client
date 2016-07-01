package org.grails.datastore.rx.rest.config

/**
 * All the settings available to configure for this implementation
 *
 * @author Graeme Rocher
 * @since 6.0
 */
interface Settings {

    /**
     * The configuration prefix
     */
    String PREFIX = "grails.gorm.rest"

    /**
     * The default host to connect to
     */
    String SETTING_HOST             = "${PREFIX}.host"

    /**
     * Setting for multiple connections
     */
    String SETTING_CONNECTIONS      = "${PREFIX}.connections"

    /**
     * The hosts to connect to in a load balanced configuration
     */
    String SETTING_HOSTS            = "${PREFIX}.hosts"
    /**
     * The character encoding to use
     */
    String SETTING_CHARSET          = "${PREFIX}.charset"
    /**
     * The default read timeout
     */
    String SETTING_READ_TIMEOUT     = "${PREFIX}.readTimeout"
    /**
     * The default wire log level
     */
    String SETTING_LOG_LEVEL        = "${PREFIX}.logLevel"

    /**
     * Configure whether blocking operations are allowed
     */
    String SETTING_ALLOW_BLOCKING_OPERATIONS        = "${PREFIX}.allowBlockingOperations"

    /**
     * The SSL session cache size
     */
    String SETTING_SSL_SESSION_CACHE_SIZE = "${PREFIX}.ssl.sessionCacheSize"

    /**
     * The SSL session timeout
     */
    String SETTING_SSL_SESSION_TIMEOUT = "${PREFIX}.ssl.sessionTimeout"

    /**
     * The SSL provider
     */
    String SETTING_SSL_PROVIDER = "${PREFIX}.ssl.provider"

    /**
     * The SSL TrustManagerFactory
     */
    String SETTING_SSL_TRUST_MANAGER_FACTORY = "${PREFIX}.ssl.trustManagerFactory"
    /**
     * The default HTTP method to use for updates. Defaults to PUT
     */
    String SETTING_UPDATE_METHOD        = "${PREFIX}.updateMethod"
    /**
     * The configuration options to create the {@link io.reactivex.netty.client.pool.PoolConfig} with (only applicable to single host configurations)
     */
    String SETTING_POOL_OPTIONS     = "${PREFIX}.pool.options"

    /**
     * The  {@link io.reactivex.netty.client.loadbalancer.LoadBalancingStrategy} to use (only applicable to multiple host configurations)
     */
    String SETTING_LOAD_BALANCE_STRATEGY     = "${PREFIX}.loadBalanceStrategy"
    /**
     * The username to ues for BASIC auth
     */
    String SETTING_USERNAME         = "${PREFIX}.username"
    /**
     * The password to use for BASIC auth
     */
    String SETTING_PASSWORD         = "${PREFIX}.password"
    /**
     * The {@link grails.gorm.rx.rest.interceptor.RequestInterceptor} instances to use
     */
    String SETTING_INTERCEPTORS     = "${PREFIX}.interceptors"

    /**
     * The {@link org.bson.codecs.Codec} instances to use
     */
    String SETTING_CODECS           = "${PREFIX}.codecs"
    /**
     * The type of query implementation to use. Either "simple" or "bson"
     */
    String SETTING_QUERY_TYPE       = "${PREFIX}.query.type"
    /**
     * The name of the parameter used to send the order (descending or ascending) to the server
     */
    String SETTING_ORDER_PARAMETER  = "${PREFIX}.parameters.order"
    /**
     * The name of the parameter used to expand nested resources
     */
    String SETTING_EXPAND_PARAMETER = "${PREFIX}.parameters.expand"
    /**
     * The name of the parameter used to send the property to sort by to the server
     */
    String SETTING_SORT_PARAMETER   = "${PREFIX}.parameters.sort"
    /**
     * The name of the parameter used to send to restrict the maximum number of results
     */
    String SETTING_MAX_PARAMETER    = "${PREFIX}.parameters.max"
    /**
     * The name of the parameter used to send the query when using "bson" query type
     */
    String SETTING_QUERY_PARAMETER  = "${PREFIX}.parameters.query"
    /**
     * The name of the parameter used to send to provide the offset for pagination
     */
    String SETTING_OFFSET_PARAMETER = "${PREFIX}.parameters.offset"
    /**
     * The default name of the order parameter
     */
    String DEFAULT_ORDER_PARAMETER  = "order"
    /**
     * The default name of the offset parameter
     */
    String DEFAULT_OFFSET_PARAMETER = "offset"
    /**
     * The default name of the sort parameter
     */
    String DEFAULT_SORT_PARAMETER   = "sort"
    /**
     * The default name of the max parameter
     */
    String DEFAULT_MAX_PARAMETER    = "max"
    /**
     * The default name of the expand parameter
     */
    String DEFAULT_EXPAND_PARAMETER = "expand"
    /**
     * The default name of the query parameter
     */
    String DEFAULT_QUERY_PARAMETER  = "q"

    /**
     * The readTimeout argument
     */
    String ARGUMENT_READ_TIMEOUT    = "readTimeout"

    /**
     * The logLevel argument
     */
    String ARGUMENT_LOG_LEVEL       = "logLevel"

    /**
     * The interceptor argument
     */
    String ARGUMENT_INTERCEPTOR     = "interceptor"

    /**
     * The method argument
     */
    String ARGUMENT_METHOD          = "method"

    /**
     * The queryType argument
     */
    String ARGUMENT_QUERY_TYPE          = "queryType"
    /**
     * The uri argument
     */
    String ARGUMENT_URI = "uri"
}