package grails.gorm.rx.rest.mapping

import groovy.transform.CompileStatic
import org.grails.datastore.mapping.config.MappingDefinition
import org.grails.datastore.rx.rest.config.Attribute
import org.grails.datastore.rx.rest.config.Endpoint

/**
 * Helps to build mapping definitions for Neo4j
 *
 * @since 6.1
 * @author Graeme Rocher
 */
@CompileStatic
class MappingBuilder {

    /**
     * Build a REST endpoint mapping
     *
     * @param mappingDefinition The closure defining the mapping
     * @return The mapping
     */
    static MappingDefinition<Endpoint, Attribute> endpoint(@DelegatesTo(Endpoint) Closure mappingDefinition) {
        new ClosureNodeMappingDefinition(mappingDefinition)
    }

    @CompileStatic
    private static class ClosureNodeMappingDefinition implements MappingDefinition<Endpoint, Attribute> {
        final Closure definition
        private Endpoint mapping

        ClosureNodeMappingDefinition(Closure definition) {
            this.definition = definition
        }

        @Override
        Endpoint configure(Endpoint existing) {
            return Endpoint.configureExisting(existing, definition)
        }

        @Override
        Endpoint build() {
            if(mapping == null) {
                Endpoint nc = new Endpoint()
                mapping = Endpoint.configureExisting(nc, definition)
            }
            return mapping
        }

    }
}
