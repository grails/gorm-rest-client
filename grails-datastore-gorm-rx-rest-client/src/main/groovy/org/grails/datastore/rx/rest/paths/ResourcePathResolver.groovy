package org.grails.datastore.rx.rest.paths

/**
 * Creates paths to resources. For example a given a Book resource this may map to a /books URI
 *
 * @author Graeme Rocher
 * @since 6.0
 */
interface ResourcePathResolver {

    /**
     * The path to the resource
     *
     * @param resource The resource
     * @return THe path the resource
     */
    String getPath(Class resource)

    /**
     * The path to the resource
     *
     * @param resource The resource
     * @param id The resource id
     *
     * @return THe path the resource
     */
    String getPath(Class resource, Serializable id)

}