package org.grails.datastore.rx.rest.codecs

import groovy.transform.InheritConstructors
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.rx.bson.codecs.QueryStateAwareCodecRegistry
import org.grails.datastore.rx.bson.codecs.RxBsonPersistentEntityCodec

/**
 * A query state aware registry for creating {@link RestEntityCodec} instances
 */
@InheritConstructors
class RestEntityCodecRegistry extends QueryStateAwareCodecRegistry {

    @Override
    protected RxBsonPersistentEntityCodec createEntityCodec(PersistentEntity entity) {
        return new RestEntityCodec(entity, this, queryState)
    }

}
