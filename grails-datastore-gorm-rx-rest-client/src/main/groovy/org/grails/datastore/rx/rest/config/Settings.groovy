package org.grails.datastore.rx.rest.config

/**
 * All the settings available to configure for this implementation
 *
 * @author Graeme Rocher
 * @since 6.0
 */
interface Settings {

    /**
     * The default host to connect to
     */
    String SETTING_HOST             = "grails.gorm.rest.host"

    /**
     * The hosts to connect to in a load balanced configuration
     */
    String SETTING_HOSTS            = "grails.gorm.rest.hosts"
    /**
     * The character encoding to use
     */
    String SETTING_CHARSET          = "grails.gorm.rest.charset"
    /**
     * The default read timeout
     */
    String SETTING_READ_TIMEOUT     = "grails.gorm.rest.readTimeout"
    /**
     * The default wire log level
     */
    String SETTING_LOG_LEVEL        = "grails.gorm.rest.logLevel"

    /**
     * The default HTTP method to use for updates. Defaults to PUT
     */
    String SETTING_UPDATE_METHOD        = "grails.gorm.rest.updateMethod"
    /**
     * The configuration options to create the {@link io.reactivex.netty.client.pool.PoolConfig} with (only applicable to single host configurations)
     */
    String SETTING_POOL_OPTIONS     = "grails.gorm.rest.pool.options"

    /**
     * The  {@link io.reactivex.netty.client.loadbalancer.LoadBalancingStrategy} to use (only applicable to multiple host configurations)
     */
    String SETTING_LOAD_BALANCE_STRATEGY     = "grails.gorm.rest.loadBalanceStrategy"
    /**
     * The username to ues for BASIC auth
     */
    String SETTING_USERNAME         = "grails.gorm.rest.username"
    /**
     * The password to use for BASIC auth
     */
    String SETTING_PASSWORD         = "grails.gorm.rest.password"
    /**
     * The {@link grails.gorm.rx.rest.interceptor.RequestInterceptor} instances to use
     */
    String SETTING_INTERCEPTORS     = "grails.gorm.rest.interceptors"
    /**
     * The type of query implementation to use. Either "simple" or "bson"
     */
    String SETTING_QUERY_TYPE       = "grails.gorm.rest.query.type"
    /**
     * The name of the parameter used to send the order (descending or ascending) to the server
     */
    String SETTING_ORDER_PARAMETER  = "grails.gorm.rest.parameters.order"
    /**
     * The name of the parameter used to expand nested resources
     */
    String SETTING_EXPAND_PARAMETER = "grails.gorm.rest.parameters.expand"
    /**
     * The name of the parameter used to send the property to sort by to the server
     */
    String SETTING_SORT_PARAMETER   = "grails.gorm.rest.parameters.sort"
    /**
     * The name of the parameter used to send to restrict the maximum number of results
     */
    String SETTING_MAX_PARAMETER    = "grails.gorm.rest.parameters.max"
    /**
     * The name of the parameter used to send the query when using "bson" query type
     */
    String SETTING_QUERY_PARAMETER  = "grails.gorm.rest.parameters.query"
    /**
     * The name of the parameter used to send to provide the offset for pagination
     */
    String SETTING_OFFSET_PARAMETER = "grails.gorm.rest.parameters.offset"
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
}