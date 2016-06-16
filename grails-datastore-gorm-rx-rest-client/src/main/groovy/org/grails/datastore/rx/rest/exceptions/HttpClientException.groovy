package org.grails.datastore.rx.rest.exceptions

import groovy.transform.CompileStatic

/**
 * Thrown when an exception occurs during REST calls
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
class HttpClientException extends RuntimeException {

    HttpClientException(String message) {
        super(message)
    }

    HttpClientException(String message, Throwable cause) {
        super(message, cause)
    }
}
