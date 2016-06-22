package org.grails.datastore.rx

import io.reactivex.netty.client.pool.PoolConfig
import org.grails.datastore.mapping.core.DatastoreUtils
import org.grails.datastore.rx.rest.config.PoolConfigBuilder
import spock.lang.Specification

/**
 * Created by graemerocher on 22/06/16.
 */
class PoolConfigBuilderSpec extends Specification {

    void "Test pool config builder"() {
        given:"A config"
        def configuration = DatastoreUtils.createPropertyResolver('grails.gorm.rest.pool.options.maxConnections':2)

        when:"the builder is used"
        PoolConfig config = new PoolConfigBuilder(configuration).build()

        then:"it is populated correctly"
        config.getLimitDeterminationStrategy().availablePermits == 2
    }
}
