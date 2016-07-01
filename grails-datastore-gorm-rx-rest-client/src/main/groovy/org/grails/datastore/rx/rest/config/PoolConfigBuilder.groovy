package org.grails.datastore.rx.rest.config

import groovy.transform.CompileStatic
import io.reactivex.netty.client.pool.PoolConfig
import org.grails.datastore.mapping.config.ConfigurationBuilder
import org.springframework.core.env.PropertyResolver

/**
 * A Builder for constructing instances of {@link PoolConfig}
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@CompileStatic
class PoolConfigBuilder extends ConfigurationBuilder<PoolConfig, PoolConfig> {
    PoolConfigBuilder(PropertyResolver propertyResolver, String configurationPrefix) {
        super(propertyResolver, configurationPrefix)
    }

    PoolConfigBuilder(PropertyResolver propertyResolver) {
        super(propertyResolver, Settings.SETTING_POOL_OPTIONS)
    }

    @Override
    protected PoolConfig createBuilder() {
        return new PoolConfig()
    }

    @Override
    protected PoolConfig toConfiguration(PoolConfig builder) {
        return builder
    }
}
