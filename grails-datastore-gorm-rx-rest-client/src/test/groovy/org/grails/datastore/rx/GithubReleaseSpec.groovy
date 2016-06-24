package org.grails.datastore.rx

import grails.gorm.annotation.Entity
import grails.gorm.rx.rest.RxRestEntity
import grails.gorm.rx.rest.interceptor.InterceptorContext
import grails.gorm.rx.rest.interceptor.RequestBuilderInterceptor
import grails.http.HttpHeader
import grails.http.MediaType
import io.reactivex.netty.protocol.http.client.HttpClientRequest
import org.grails.datastore.mapping.core.DatastoreUtils
import org.grails.datastore.rx.rest.RxRestDatastoreClient
import org.springframework.core.env.PropertyResolver
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

/**
 * Created by graemerocher on 24/06/16.
 */
class GithubReleaseSpec extends Specification {

    @Shared PropertyResolver configuration = DatastoreUtils.createPropertyResolver(
            (RxRestDatastoreClient.SETTING_HOST): "https://api.github.com",
            (RxRestDatastoreClient.SETTING_LOG_LEVEL): "TRACE",
            (RxRestDatastoreClient.SETTING_INTERCEPTORS):OauthInterceptor.name
    )
    @Shared @AutoCleanup RxRestDatastoreClient client = new RxRestDatastoreClient(configuration, Release)


    void "test read release from Github API"() {
        when:"A release is read"
        Release release = Release.where {
            eq 'owner', 'grails'
            eq 'repo', 'grails-core'
        }.findAll().first().toBlocking().first()

        then:"The release is valid"
        release != null
        release.name
        release.url
    }


    static class OauthInterceptor extends RequestBuilderInterceptor {
        @Override
        Closure build(HttpClientRequest request, InterceptorContext context) {
            buildRequest {
                auth("token ${System.getenv('GH_TOKEN')}")
                header(HttpHeader.USER_AGENT, "RxGORM REST Client")
            }
        }
    }
}

@Entity
class Release implements RxRestEntity<Release> {
    String name
    String url

    static mapping = {
        uri "/repos/{owner}/{repo}/releases"
        contentType MediaType.JSON
    }
}


