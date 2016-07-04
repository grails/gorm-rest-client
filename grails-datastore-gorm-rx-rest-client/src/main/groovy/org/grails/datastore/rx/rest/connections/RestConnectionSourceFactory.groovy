package org.grails.datastore.rx.rest.connections

import groovy.transform.CompileStatic
import io.reactivex.netty.client.ConnectionProviderFactory
import io.reactivex.netty.client.loadbalancer.LoadBalancerFactory
import io.reactivex.netty.client.pool.PoolConfig
import io.reactivex.netty.client.pool.SingleHostPoolingProviderFactory
import org.grails.datastore.mapping.core.connections.ConnectionSource
import org.grails.datastore.mapping.core.connections.ConnectionSourceFactory
import org.grails.datastore.mapping.core.connections.DefaultConnectionSource
import org.grails.datastore.rx.rest.config.Settings
import org.springframework.core.env.PropertyResolver

/**
 * A factory for creating {@link org.grails.datastore.mapping.core.connections.ConnectionSource} instances for REST
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
class RestConnectionSourceFactory implements ConnectionSourceFactory<ConnectionProviderFactory, RestConnectionSourceSettings> {
    @Override
    ConnectionSource<ConnectionProviderFactory, RestConnectionSourceSettings> create(String name, PropertyResolver configuration, RestConnectionSourceSettings fallback = null) {
        String prefix = ConnectionSource.DEFAULT == name ? Settings.PREFIX : Settings.SETTING_CONNECTIONS + ".$name"
        RestConnectionSourceSettingsBuilder settingsBuilder = new RestConnectionSourceSettingsBuilder(configuration, prefix, fallback)
        RestConnectionSourceSettings settings = settingsBuilder.build()

        return create( name, settings )
    }

    ConnectionSource<ConnectionProviderFactory, RestConnectionSourceSettings> create(String name, RestConnectionSourceSettings settings) {
        ConnectionProviderFactory connectionProviderFactory

        List<String> hosts = settings.hosts
        if (hosts.isEmpty()) {
            hosts.add('http://localhost:8080')
        }

        if (hosts.size() == 1) {
            if (settings.pool.options != null) {
                connectionProviderFactory = SingleHostPoolingProviderFactory.create(settings.pool.options.build())
            } else {
                connectionProviderFactory = SingleHostPoolingProviderFactory.create(new PoolConfig())
            }
        } else {
            connectionProviderFactory = LoadBalancerFactory.create(settings.loadBalanceStrategy)
        }
        return create(name, connectionProviderFactory, settings);
    }

    protected DefaultConnectionSource<ConnectionProviderFactory, RestConnectionSourceSettings> create(String name, ConnectionProviderFactory connectionProviderFactory, RestConnectionSourceSettings settings) {
        return new DefaultConnectionSource<ConnectionProviderFactory, RestConnectionSourceSettings>(name, connectionProviderFactory, settings)
    }

    @Override
    Serializable getConnectionSourcesConfigurationKey() {
        return Settings.SETTING_CONNECTIONS
    }
}
