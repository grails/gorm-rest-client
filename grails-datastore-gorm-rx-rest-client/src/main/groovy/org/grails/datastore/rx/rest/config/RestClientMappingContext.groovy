package org.grails.datastore.rx.rest.config

import groovy.transform.CompileStatic
import org.bson.codecs.BsonValueCodecProvider
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
import org.grails.datastore.rx.rest.codecs.RestEntityCodec
import org.grails.datastore.rx.rest.codecs.VndErrorsCodec
import org.springframework.core.env.PropertyResolver
import org.springframework.validation.Errors


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

    private final Map<Class, Codec> codecs = [:]

    RestClientMappingContext(Class...classes) {
        this(null, classes)
    }

    RestClientMappingContext(PropertyResolver configuration, Class...classes) {
        if(configuration != null) {
            configure(configuration)
        }
        addPersistentEntities(classes)
        this.codecRegistry = CodecRegistries.fromProviders(new CodecExtensions(), this, new BsonValueCodecProvider())
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
            return new RestEntityCodec(entity, registry)
        }
        else {
            Codec<T> codec = codecs.get(clazz)
            if(codec != null) {
                return codec
            }
            else if(Errors.isAssignableFrom(clazz)) {
                return (Codec<T>)new VndErrorsCodec()
            }
        }
        return null
    }

    /**
     * Adds a new codec
     *
     * @param codec The codec
     */
    void addCodec(Codec codec) {
        codecs.put(codec.encoderClass, codec)
    }
}
