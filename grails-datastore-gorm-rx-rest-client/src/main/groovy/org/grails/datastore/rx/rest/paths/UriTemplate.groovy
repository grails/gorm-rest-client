package org.grails.datastore.rx.rest.paths

import groovy.transform.CompileStatic

/**
 *
 * Represents a URI template
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
class UriTemplate {

    final String template
    final List<String> variables

    UriTemplate(String template) {
        this.template = template
        this.variables = parse(template)
    }

    protected List<String> parse(String template) {
        char[] chars = template.chars

        for(char c in chars) {

        }
    }
}
