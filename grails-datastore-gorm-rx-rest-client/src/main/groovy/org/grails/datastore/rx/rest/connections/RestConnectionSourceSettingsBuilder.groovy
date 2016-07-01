package org.grails.datastore.rx.rest.connections

import groovy.transform.CompileStatic
import org.grails.datastore.mapping.config.ConfigurationBuilder
import org.grails.datastore.rx.rest.config.Settings
import org.springframework.core.env.PropertyResolver

/**
 * A builder for {@link RestConnectionSourceSettings}
 */
@CompileStatic
class RestConnectionSourceSettingsBuilder extends ConfigurationBuilder<RestConnectionSourceSettings, RestConnectionSourceSettings> {
    RestConnectionSourceSettingsBuilder(PropertyResolver propertyResolver, String configurationPrefix) {
        super(propertyResolver, configurationPrefix)
    }

    RestConnectionSourceSettingsBuilder(PropertyResolver propertyResolver) {
        super(propertyResolver, Settings.PREFIX)
    }

    @Override
    protected RestConnectionSourceSettings createBuilder() {
        return new RestConnectionSourceSettings()
    }

    @Override
    protected RestConnectionSourceSettings toConfiguration(RestConnectionSourceSettings builder) {
        return builder
    }
}
