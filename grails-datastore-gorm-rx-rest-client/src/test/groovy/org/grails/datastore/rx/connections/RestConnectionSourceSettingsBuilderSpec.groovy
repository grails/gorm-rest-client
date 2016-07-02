package org.grails.datastore.rx.connections

import io.reactivex.netty.client.pool.PoolConfig
import org.grails.datastore.mapping.core.DatastoreUtils
import org.grails.datastore.rx.rest.config.PoolConfigBuilder
import org.grails.datastore.rx.rest.connections.RestConnectionSourceSettings
import org.grails.datastore.rx.rest.connections.RestConnectionSourceSettingsBuilder
import spock.lang.Specification

/**
 * Created by graemerocher on 02/07/16.
 */
class RestConnectionSourceSettingsBuilderSpec extends Specification {

    void "Test pool config builder"() {
        given:"A config"
        def configuration = DatastoreUtils.createPropertyResolver('grails.gorm.rest.pool.options.maxConnections':2)
        def builder = new RestConnectionSourceSettingsBuilder(configuration)


        when:"the builder is used"
        RestConnectionSourceSettings settings = builder.build()
        PoolConfig config = settings.pool.options.build()

        then:"it is populated correctly"
        config.getLimitDeterminationStrategy().availablePermits == 2
    }
}
