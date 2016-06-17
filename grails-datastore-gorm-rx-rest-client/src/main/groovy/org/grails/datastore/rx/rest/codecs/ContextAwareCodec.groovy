package org.grails.datastore.rx.rest.codecs

import org.bson.codecs.Codec

/**
 * A codec that is aware of a context
 *
 * @author Graeme Rocher
 * @since 6.0
 */
interface ContextAwareCodec<T> extends Codec<T> {

    void setContext(Object context)
}