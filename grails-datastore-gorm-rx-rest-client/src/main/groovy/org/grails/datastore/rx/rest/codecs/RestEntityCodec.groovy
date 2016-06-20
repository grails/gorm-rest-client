package org.grails.datastore.rx.rest.codecs

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import org.bson.BsonReader
import org.bson.BsonType
import org.bson.codecs.Codec
import org.bson.codecs.DecoderContext
import org.grails.datastore.bson.codecs.BsonPersistentEntityCodec
import org.grails.datastore.gorm.schemaless.DynamicAttributes
import org.grails.datastore.mapping.model.PersistentProperty
import org.grails.datastore.mapping.model.types.Association
import org.grails.datastore.mapping.model.types.ToMany
import org.grails.datastore.mapping.model.types.ToOne
import org.grails.datastore.mapping.reflect.EntityReflector
import org.grails.datastore.rx.rest.json.HalConstants

/**
 * Extends the default {@link BsonPersistentEntityCodec} with additional handling for HAL
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@CompileStatic
@InheritConstructors
class RestEntityCodec extends BsonPersistentEntityCodec {

    @Override
    protected void readSchemaless(BsonReader jsonReader, DynamicAttributes dynamicAttributes, String name, DecoderContext decoderContext) {
        if(HalConstants.EMBEDDED.equals(name)) {
            BsonType bsonType = jsonReader.getCurrentBsonType()
            if(bsonType == BsonType.DOCUMENT) {
                jsonReader.readStartDocument()

                bsonType = jsonReader.readBsonType()
                EntityReflector entityReflector = entity.reflector
                while(bsonType != BsonType.END_OF_DOCUMENT) {
                    String associationName = jsonReader.readName()
                    PersistentProperty property = entity.getPropertyByName(associationName)
                    if(property instanceof Association) {
                        Association association = (Association)property
                        bsonType = jsonReader.currentBsonType
                        Codec codec = codecRegistry.get(association.getAssociatedEntity().javaClass)
                        if(codec != null) {
                            if(bsonType == BsonType.DOCUMENT && property instanceof ToOne) {
                                def decoded = codec.decode(jsonReader, DEFAULT_DECODER_CONTEXT)
                                entityReflector.setProperty(dynamicAttributes, associationName, decoded)
                            }
                            else if(bsonType == BsonType.ARRAY && property instanceof ToMany) {
                                jsonReader.readStartArray()
                                List allDecoded = []
                                while(bsonType != BsonType.END_OF_DOCUMENT) {
                                    def decoded = codec.decode(jsonReader, DEFAULT_DECODER_CONTEXT)
                                    allDecoded.add(decoded)
                                }
                                entity.mappingContext.createEntityAccess(entity, dynamicAttributes).setProperty(associationName, allDecoded)
                                jsonReader.readEndArray()
                            }
                            else {
                                jsonReader.skipValue()
                            }
                        }
                        else {
                            jsonReader.skipValue()
                        }
                    }
                    else {
                        jsonReader.skipValue()
                    }
                    bsonType = jsonReader.readBsonType()
                }
                jsonReader.readEndDocument()
            }
        }
        else {
            super.readSchemaless(jsonReader, dynamicAttributes, name, decoderContext)
        }
    }
}
