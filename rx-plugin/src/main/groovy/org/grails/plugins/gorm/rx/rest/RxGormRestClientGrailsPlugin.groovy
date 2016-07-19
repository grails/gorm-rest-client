package org.grails.plugins.gorm.rx.rest

import grails.core.GrailsClass
import grails.gorm.rx.rest.RxRestEntity
import grails.plugins.Plugin
import org.grails.core.artefact.DomainClassArtefactHandler
import org.grails.datastore.rx.rest.RxRestDatastoreClient
import org.grails.plugins.web.rx.mvc.*

/**
 * A Grails plugin for RxGORM for REST
 *
 * @author Graeme Rocher
 * @since 1.0
 */
class RxGormRestClientGrailsPlugin extends Plugin {
    def license = "Apache 2.0 License"
    def organization = [name: "Grails", url: "http://grails.org/"]
    def developers = [
            [name: "Graeme Rocher", email: "graeme@grails.org"]]
    def issueManagement = [system: "Github", url: "https://github.com/grails/gorm-rest-client/issues"]
    def scm = [url: "https://github.com/grails/gorm-rest-client"]

    def grailsVersion = "3.0.0 > *"
    def observe = ['services', 'domainClass']
    def loadAfter = ['domainClass', 'hibernate', 'hibernate4', 'services']
    def author = "Graeme Rocher"
    def authorEmail = "graeme@grails.org"
    def title = "RxGORM for REST"
    def description = 'A plugin that adds RxGORM for REST integration for Grails applications'
    def documentation = "http://gorm.grails.org/rx/rest-client/"

    @Override
    Closure doWithSpring() {
        {->
            def classes = grailsApplication.getArtefacts(DomainClassArtefactHandler.TYPE).findAll() { GrailsClass cls ->
                RxRestEntity.isAssignableFrom(cls.clazz)
            }.collect() { GrailsClass cls -> cls.clazz }
            rxRestClientDatastoreClient(RxRestDatastoreClient, config, classes as Class[])
            rxHttpClientBuilder(rxRestClientDatastoreClient:"getRxHttpClientBuilder")
            rxAsyncResultTransformer(RxResultTransformer)
        }
    }
}
