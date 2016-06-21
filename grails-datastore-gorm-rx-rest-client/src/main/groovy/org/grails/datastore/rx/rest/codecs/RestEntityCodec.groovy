package org.grails.datastore.rx.rest.codecs

import com.damnhandy.uri.template.UriTemplate
import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import org.bson.BsonDocument
import org.bson.BsonReader
import org.bson.BsonType
import org.bson.codecs.Codec
import org.bson.codecs.DecoderContext
import org.grails.datastore.bson.codecs.BsonPersistentEntityCodec
import org.grails.datastore.gorm.schemaless.DynamicAttributes
import org.grails.datastore.mapping.engine.EntityAccess
import org.grails.datastore.mapping.model.PersistentProperty
import org.grails.datastore.mapping.model.types.Association
import org.grails.datastore.mapping.model.types.ToMany
import org.grails.datastore.mapping.model.types.ToOne
import org.grails.datastore.mapping.query.Query
import org.grails.datastore.mapping.reflect.EntityReflector
import org.grails.datastore.rx.bson.codecs.RxBsonPersistentEntityCodec
import org.grails.datastore.rx.collection.RxCollectionUtils
import org.grails.datastore.rx.rest.RxRestDatastoreClient
import org.grails.datastore.rx.rest.RestEndpointPersistentEntity
import org.grails.datastore.rx.rest.json.HalConstants
import org.grails.gorm.rx.api.RxGormEnhancer

/**
 * Extends the default {@link BsonPersistentEntityCodec} with additional handling for HAL
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@CompileStatic
@InheritConstructors
class RestEntityCodec extends RxBsonPersistentEntityCodec {

    @Override
    protected void readingComplete(EntityAccess access) {
        def owner = entity.javaClass
        def staticApi = RxGormEnhancer.findStaticApi(owner)

        DynamicAttributes dynamicAttributes = (DynamicAttributes) access.entity
        Map<String, Object> halLinks = (Map<String, Object>) dynamicAttributes.getAt(HalConstants.LINKS)
        RxRestDatastoreClient datastoreClient = (RxRestDatastoreClient) staticApi.datastoreClient
        Set<String> halLinkAssociationNames = halLinks != null ? halLinks.keySet() : Collections.<String> emptySet()
        for (String associationName in halLinkAssociationNames) {

            def property = entity.getPropertyByName(associationName)
            def existing = access.getProperty(associationName)
            if (existing != null) {
                continue
            }

            String uriToken = "{${entity.decapitalizedName}}"
            EntityReflector entityReflector = entity.reflector
            Serializable entityId = (Serializable) entityReflector.getIdentifier(dynamicAttributes)
            if (property instanceof Association && entityId != null) {
                Association association = (Association) property
                BsonDocument decoded = (BsonDocument) halLinks.get(associationName)

                String uriStr = decoded.getString(HalConstants.HREF).value
                if (uriStr.contains(uriToken)) {

                    uriStr = uriStr.replace(uriToken, entityId.toString())
                    def uriTemplate = UriTemplate.fromTemplate(uriStr)
                    if (property instanceof ToOne) {

                        Query query = prepareQuery(datastoreClient, association, uriTemplate, entityReflector, dynamicAttributes)
                        entityReflector.setProperty(
                                dynamicAttributes,
                                associationName,
                                datastoreClient.proxy(query)
                        )
                    } else if (property instanceof ToMany) {
                        Query query = prepareQuery(datastoreClient, association, uriTemplate, entityReflector, dynamicAttributes)
                        Collection lazyCollection = RxCollectionUtils.createConcreteCollection(association, query, queryState)
                        entityReflector.setProperty(
                                dynamicAttributes,
                                associationName,
                                lazyCollection
                        )
                    }
                }
            }
        }

        for (Association association in entity.associations) {
            if (association.isBidirectional()) {
                if (halLinkAssociationNames.contains(association.name)) continue

                boolean toMany = association instanceof ToMany
                if ( toMany ||  ((association instanceof ToOne) && ((ToOne) association).isForeignKeyInChild())) {
                    String associationName = association.name
                    def existing = access.getProperty(associationName)
                    if (existing == null) {

                        RxRestDatastoreClient client = (RxRestDatastoreClient) staticApi.datastoreClient

                        Class associatedType = association.associatedEntity.javaClass
                        RestEndpointPersistentEntity restEntity = (RestEndpointPersistentEntity)this.entity
                        UriTemplate uri = restEntity.getAssociationTemplate(association.name)
                        Query query
                        String inversePropertyName = association.inverseSide.name
                        if (uri != null) {
                            query = client.createQuery(associatedType, uri, queryState)
                            if(uri.variables.contains(inversePropertyName)) {
                                query.eq(inversePropertyName, access.entity)
                            }
                        } else {
                            query = client.createQuery(associatedType, queryState)
                            query.eq(inversePropertyName, access.entity)
                        }

                        if(toMany) {
                            populateUriTemplateVariables(query, entity.reflector, uri, dynamicAttributes)
                            access.setPropertyNoConversion(associationName, RxCollectionUtils.createConcreteCollection(association, query, queryState))
                        }
                        else {
                            def proxy = client.proxy(query)
                            access.setPropertyNoConversion(associationName, proxy)
                        }
                    }

                }
            }
        }

        super.readingComplete(access)
    }

    protected Query prepareQuery(RxRestDatastoreClient datastoreClient, Association association, UriTemplate uriTemplate, EntityReflector entityReflector, DynamicAttributes dynamicAttributes) {
        Query query = datastoreClient.createQuery(association.associatedEntity.javaClass, uriTemplate, queryState)
        return populateUriTemplateVariables(query, entityReflector, uriTemplate, dynamicAttributes)
    }

    protected Query populateUriTemplateVariables(Query query, EntityReflector entityReflector, UriTemplate uriTemplate, DynamicAttributes dynamicAttributes) {
        for (var in uriTemplate.variables) {

            def property = entity.getPropertyByName(var)
            if(property != null) {
                EntityReflector.PropertyReader reader = entityReflector.getPropertyReader(var)
                if (reader != null) {
                    query.eq(var, reader.read(dynamicAttributes))
                }
            }
        }
        return query
    }

    @Override
    protected void readSchemaless(BsonReader jsonReader, DynamicAttributes dynamicAttributes, String name, DecoderContext decoderContext) {
        if (HalConstants.LINKS.equals(name)) {
            BsonType bsonType = jsonReader.getCurrentBsonType()
            if (bsonType == BsonType.DOCUMENT) {
                jsonReader.readStartDocument()

                Map halLinks = [:]
                dynamicAttributes.putAt(HalConstants.LINKS, halLinks)

                Codec codec = codecRegistry.get(BsonDocument)
                bsonType = jsonReader.readBsonType()

                while (bsonType != BsonType.END_OF_DOCUMENT) {
                    String associationName = jsonReader.readName()
                    PersistentProperty property = entity.getPropertyByName(associationName)
                    if (property instanceof Association) {
                        bsonType = jsonReader.currentBsonType
                        if (codec != null) {
                            if (bsonType == BsonType.DOCUMENT) {
                                BsonDocument decoded = codec.decode(jsonReader, DEFAULT_DECODER_CONTEXT)
                                halLinks.put(associationName, decoded)
                            } else {
                                jsonReader.skipValue()
                            }
                        } else {
                            jsonReader.skipValue()
                        }
                    } else {
                        jsonReader.skipValue()
                    }
                    bsonType = jsonReader.readBsonType()
                }
                jsonReader.readEndDocument()
            } else {
                jsonReader.skipValue()
            }
        } else if (HalConstants.EMBEDDED.equals(name)) {
            BsonType bsonType = jsonReader.getCurrentBsonType()
            if (bsonType == BsonType.DOCUMENT) {
                jsonReader.readStartDocument()

                bsonType = jsonReader.readBsonType()
                EntityReflector entityReflector = entity.reflector
                while (bsonType != BsonType.END_OF_DOCUMENT) {
                    String associationName = jsonReader.readName()
                    PersistentProperty property = entity.getPropertyByName(associationName)
                    if (property instanceof Association) {
                        Association association = (Association) property
                        bsonType = jsonReader.currentBsonType
                        Codec codec = codecRegistry.get(association.getAssociatedEntity().javaClass)
                        if (codec != null) {
                            if (bsonType == BsonType.DOCUMENT && property instanceof ToOne) {
                                def decoded = codec.decode(jsonReader, DEFAULT_DECODER_CONTEXT)
                                entityReflector.setProperty(dynamicAttributes, associationName, decoded)
                            } else if (bsonType == BsonType.ARRAY && property instanceof ToMany) {
                                jsonReader.readStartArray()
                                List allDecoded = []
                                while (bsonType != BsonType.END_OF_DOCUMENT) {
                                    def decoded = codec.decode(jsonReader, DEFAULT_DECODER_CONTEXT)
                                    allDecoded.add(decoded)
                                }
                                entity.mappingContext.createEntityAccess(entity, dynamicAttributes).setProperty(associationName, allDecoded)
                                jsonReader.readEndArray()
                            } else {
                                jsonReader.skipValue()
                            }
                        } else {
                            jsonReader.skipValue()
                        }
                    } else {
                        jsonReader.skipValue()
                    }
                    bsonType = jsonReader.readBsonType()
                }
                jsonReader.readEndDocument()
            }
        } else {
            super.readSchemaless(jsonReader, dynamicAttributes, name, decoderContext)
        }
    }
}
