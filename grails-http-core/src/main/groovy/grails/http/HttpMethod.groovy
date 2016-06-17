package grails.http

import groovy.transform.CompileStatic

/**
 * An enum containing the valid HTTP methods
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@CompileStatic
enum HttpMethod implements CharSequence {
    OPTIONS,
    GET,
    POST,
    PUT,
    PATCH,
    DELETE,
    HEAD,

    @Override
    int length() {
        name().length()
    }

    @Override
    char charAt(int index) {
        return name().charAt(index)
    }

    @Override
    CharSequence subSequence(int start, int end) {
        return name().subSequence(start, end)
    }
}