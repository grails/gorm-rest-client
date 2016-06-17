package org.grails.datastore.rx.rest.codecs

import org.bson.BsonReader
import org.bson.BsonType
import org.bson.BsonWriter
import org.bson.codecs.Codec
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext
import org.grails.datastore.mapping.validation.ValidationErrors
import org.springframework.validation.Errors
import org.springframework.validation.ObjectError

/**
 * Decodes vnd.error objects into {@link Errors}
 *
 * @author Graeme Rocher
 * @since 6.0
 */
class VndErrorsCodec implements ContextAwareCodec<Errors>{

    Object target

    @Override
    void setContext(Object obj) {
        this.target = obj
    }

    @Override
    Errors decode(BsonReader reader, DecoderContext decoderContext) {
        if(target == null) {
            throw new IllegalStateException("Target object cannot be null")
        }

        def className = target.getClass().simpleName
        ValidationErrors errors = new ValidationErrors(target, className)

        reader.readStartDocument()
        def name = reader.readName()
        if('message' == name) {
            // single error
            String message = reader.readString()
            def bsonType = reader.readBsonType()

            errors.addError(new ObjectError(className, message))
        }
        else if('total' == name) {
            // multiple errors
            // TODO:
        }
        else {
            throw new IllegalArgumentException("Invalid vnd.error format returned from server")
        }

        reader.readEndDocument()
        return errors
    }

    @Override
    void encode(BsonWriter writer, Errors value, EncoderContext encoderContext) {
        throw new UnsupportedOperationException("Encoding not yet supported")
    }

    @Override
    Class<Errors> getEncoderClass() {
        return Errors
    }
}
