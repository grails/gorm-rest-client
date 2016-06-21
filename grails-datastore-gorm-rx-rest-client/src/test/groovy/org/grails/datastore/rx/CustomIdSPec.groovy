package org.grails.datastore.rx

import grails.http.HttpMethod
import grails.http.MediaType
import org.grails.datastore.rx.domain.Person
import org.grails.datastore.rx.domain.Product
import rx.Observable

/**
 * Created by graemerocher on 21/06/16.
 */
class CustomIdSpec extends RxGormSpec {
    @Override
    List<Class> getDomainClasses() {
        [Product]
    }

    void "Test get an entity using a GET request"() {
        given:"A canned response"
        def mock = client.expect {
            uri '/product/MacBook'
            accept(MediaType.JSON)
        }
        .respond {
            json {
                id 1
                name "MacBook"
            }
        }

        when:"A get request is issued"
        Observable<Product> observable = Product.get("MacBook")
        Product p = observable.toBlocking().first()

        then:"The result is correct"
        mock.verify()
        p.name == "MacBook"
    }

    void "Test that mapping an object with a custom id strategy works"() {
        given:"A canned response"
        def mock = client.expect {
            uri '/product/MacBook'
            method HttpMethod.PUT
            json {
                name "MacBook"
            }
        }
        .respond {
            ok()
            json {
                id 1
                name "MacBook"
            }
        }

        when:"A get request is issued"
        def sw = new StringWriter()
        def date = new Date().parse('yyyy/MM/dd', '1973/07/09')

        Product p = new Product(name: "MacBook")
        p.id = 1L
        p = p.save().toBlocking().first()

        then:"The result is correct"
        mock.verify()
        p.id == 1L
    }
}
