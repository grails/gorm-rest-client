package org.grails.datastore.rx.rest.codecs

import groovy.transform.InheritConstructors
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.rx.bson.codecs.QueryStateAwareCodeRegistry
import org.grails.datastore.rx.bson.codecs.RxBsonPersistentEntityCodec

/**
 * A query state aware registry for creating {@link RestEntityCodec} instances
 */
@InheritConstructors
class RestEntityCodeRegistry extends QueryStateAwareCodeRegistry {

    @Override
    protected RxBsonPersistentEntityCodec createEntityCodec(PersistentEntity entity) {
        return new RestEntityCodec(entity, this, queryState)
    }
}
