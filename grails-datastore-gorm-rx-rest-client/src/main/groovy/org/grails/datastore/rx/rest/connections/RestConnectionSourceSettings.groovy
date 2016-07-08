package org.grails.datastore.rx.rest.connections

import grails.http.HttpMethod
import groovy.transform.AutoClone
import groovy.transform.builder.Builder
import groovy.transform.builder.SimpleStrategy
import io.netty.handler.logging.LogLevel
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslProvider
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import io.reactivex.netty.client.loadbalancer.LoadBalancingStrategy
import io.reactivex.netty.protocol.http.client.loadbalancer.EWMABasedP2CStrategy
import org.grails.datastore.mapping.core.connections.ConnectionSourceSettings
import org.grails.datastore.rx.rest.config.PoolConfigBuilder
import org.grails.datastore.rx.rest.config.Settings

import javax.net.ssl.TrustManagerFactory
/**
 * Settings for RxGORM for REST
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@AutoClone
@Builder(builderStrategy = SimpleStrategy, prefix = '')
class RestConnectionSourceSettings extends ConnectionSourceSettings implements Settings {

    /**
     * The default read timeout
     */
    Integer readTimeout = -1

    /**
     * The default log level
     */
    LogLevel logLevel
    /**
     * The default client host to connect to
     */
    List<String> hosts = []

    /**
     * The username to use for BASIC auth
     */
    String username

    /**
     * The password to use for BASIC auth
     */
    String password

    /**
     * Whether to allow blocking operations
     */
    boolean allowBlockingOperations = false

    /**
     * The encoding to use
     */
    String charset = "UTF-8"


    /**
     * The interceptors to use
     */
    List interceptors = []

    /**
     * The default method to use for updates. Defaults to PUT
     */
    HttpMethod defaultUpdateMethod = HttpMethod.PUT

    /**
     * The SSL provider
     */
    SslProvider sslProvider = SslContext.defaultClientProvider()

    /**
     * The trust manager factory
     */
    TrustManagerFactory sslTrustManagerFactory = InsecureTrustManagerFactory.INSTANCE

    /**
     * The default session cache size
     */
    Long sslSessionCacheSize = -1L

    /**
     * The SSL timeout period
     */
    Long sslSessionTimeout = -1L

    /**
     * Settings for the connection pool
     */
    PoolSettings pool = new PoolSettings()
    /**
     * The http proxies to use
     */
    List<Proxy> proxies

    /**
     * The load balance strategy to use
     */
    LoadBalancingStrategy loadBalanceStrategy = new EWMABasedP2CStrategy()

    /**
     * Parameter settings
     */
    ParameterSettings parameters = new ParameterSettings()

    /**
     * Parameter settings
     */
    QuerySettings query = new QuerySettings()


    /**
     * @return Whether the connection is secure
     */
    boolean isSecure() {
        hosts.any { Object o -> o.toString().startsWith("https") }
    }
    /**
     * Sets a single host
     *
     * @param host The host to set
     */
    void host(String host) {
        if(host != null) {
            hosts.clear()
            hosts.add(host)
        }
    }

    @AutoClone
    @Builder(builderStrategy = SimpleStrategy, prefix = '')
    static class ParameterSettings {
        /**
         * The name of the order parameter to use
         */
        String order = DEFAULT_ORDER_PARAMETER

        /**
         * The name of the query parameter to use
         */
        String query = DEFAULT_QUERY_PARAMETER

        /**
         * The name of the offset parameter to use
         */
        String offset = DEFAULT_OFFSET_PARAMETER

        /**
         * The name of the max parameter to use
         */
        String max = DEFAULT_MAX_PARAMETER

        /**
         * The name of the sort parameter to use
         */
        String sort = DEFAULT_SORT_PARAMETER

        /**
         * The name of the expand parameter to use
         */
        String expand = DEFAULT_EXPAND_PARAMETER

    }

    @AutoClone
    @Builder(builderStrategy = SimpleStrategy, prefix = '')
    static class PoolSettings {
        /**
         * Pool config options
         */
        PoolConfigBuilder options
    }

    @AutoClone
    @Builder(builderStrategy = SimpleStrategy, prefix = '')
    static class QuerySettings {
        /**
         * The name of the order parameter to use
         */
        String type = "simple"
    }
}
