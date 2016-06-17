package grails.http

import spock.lang.Specification

/**
 * Created by graemerocher on 17/06/16.
 */
class MediaTypeSpec extends Specification {

    void "Test media type handling"() {

        when:
        def type = new MediaType("application/vnd.error+json;q=1.1")
        def another = new MediaType("application/vnd.error+json;q=1.0")

        then:
        type.name == "application/vnd.error+json"
        type.subtype == "vnd.error+json"
        type.extension == "json"
        type.quality == "1.1"
        type == another
    }
}
