package org.grails.datastore.rx.rest.config

import groovy.transform.CompileStatic
import org.bson.codecs.Codec
import org.bson.codecs.configuration.CodecProvider
import org.bson.codecs.configuration.CodecRegistries
import org.bson.codecs.configuration.CodecRegistry
import org.grails.datastore.bson.codecs.BsonPersistentEntityCodec
import org.grails.datastore.bson.codecs.CodecExtensions
import org.grails.datastore.mapping.config.Property
import org.grails.datastore.mapping.model.AbstractMappingContext
import org.grails.datastore.mapping.model.MappingConfigurationStrategy
import org.grails.datastore.mapping.model.MappingFactory
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.config.GormMappingConfigurationStrategy

/**
 * A {@link org.grails.datastore.mapping.model.MappingContext} for REST Client applications
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
class RestClientMappingContext extends AbstractMappingContext implements CodecProvider {
    final MappingFactory<Endpoint, Property> mappingFactory = new RestClientMappingFactory()
    final MappingConfigurationStrategy mappingSyntaxStrategy = new GormMappingConfigurationStrategy(mappingFactory)
    final CodecRegistry codecRegistry

    RestClientMappingContext(Class...classes) {
        addPersistentEntities(classes)
        this.codecRegistry = CodecRegistries.fromProviders(new CodecExtensions(), this)
    }

    @Override
    protected PersistentEntity createPersistentEntity(Class javaClass) {
        return new RestEndpointPersistentEntity(javaClass, this)
    }

    @Override
    RestEndpointPersistentEntity getPersistentEntity(String name) {
        return (RestEndpointPersistentEntity)super.getPersistentEntity(name)
    }

    @Override
    def <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {
        PersistentEntity entity = getPersistentEntity(clazz.name)
        if(entity != null) {
            return new BsonPersistentEntityCodec(codecRegistry, entity)
        }
        return null
    }
}
