package org.grails.datastore.rx

import grails.http.HttpMethod
import grails.http.MediaType
import org.grails.datastore.rx.domain.CommonTypes


/**
 * @author graemerocher
 */
class CommonTypesSpec extends RxGormSpec {

    def "test persist basic types"() {
        given:
        def now = new Date()
        def cal = new GregorianCalendar()

        def ct = new CommonTypes(
                l: 10L,
                b: 10 as byte,
                s: 10 as short,
                bool: true,
                i: 10,
                url: new URL("http://google.com"),
                date: now,
                c: cal,
                bd: 1.0,
                bi: 10 as BigInteger,
                d: 1.0 as Double,
                f: 1.0 as Float,
                tz: TimeZone.getTimeZone("GMT"),
                loc: Locale.UK,
                cur: Currency.getInstance("USD"),
                ba: 'hello'.bytes
        )

        def mock = client.expect {
            uri '/commonTypes'
            method HttpMethod.POST
            json ct.toJson()
        }
        .respond {
            created()
        }



        when:
        ct.save().toBlocking().first()

        then:
        mock.verify()
    }

    @Override
    List<Class> getDomainClasses() {
        [CommonTypes]
    }
}

