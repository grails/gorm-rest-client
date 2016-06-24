package org.grails.datastore.rx

import grails.http.HttpMethod
import org.bson.BsonReader
import org.bson.BsonWriter
import org.bson.codecs.Codec
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext
import org.grails.datastore.mapping.core.DatastoreUtils
import org.grails.datastore.rx.domain.Birthday
import org.grails.datastore.rx.domain.BirthdayPerson
import org.grails.datastore.rx.domain.Person
import org.grails.datastore.rx.rest.config.Settings
import org.grails.datastore.rx.rest.test.TestRxRestDatastoreClient

/**
 * Created by graemerocher on 24/06/16.
 */
class CustomCodecSpec extends RxGormSpec {
    @Override
    List<Class> getDomainClasses() {
        [BirthdayPerson]
    }

    void "Test custom codecs"() {
        given:"A a person with a custom type"
        def sw = new StringWriter()
        def date = new Date().parse('yyyy/MM/dd', '1973/07/09')

        BirthdayPerson p = new BirthdayPerson(name: "Fred", birthday: new Birthday(date: date))
        p.id = 1L
        def mock = client.expect {
            uri '/birthdayPerson/1'
            method HttpMethod.PATCH
            json p.toJson()
        }
        .respond {
            ok()
            json {
                id 1
                name "Fred"
                birthday "2006-07-09T00:00+0000"
            }
        }

        when:"A get request is issued"

        p = p.patch().toBlocking().first()

        then:"The result is correct"
        mock.verify()
        p.id == 1L
    }

    @Override
    protected TestRxRestDatastoreClient createRestDatastoreClient(List<Class> classes) {
        DatastoreUtils.createPropertyResolver((Settings.SETTING_CODECS):'org.grails.datastore.rx.CustomCodecSpec.BirthdayCodec')
        return new TestRxRestDatastoreClient(classes as Class[])
    }

    static class BirthdayCodec implements Codec<Birthday> {

        @Override
        Birthday decode(BsonReader reader, DecoderContext decoderContext) {
            return new Birthday(date: new Date(reader.readDateTime()))
        }

        @Override
        void encode(BsonWriter writer, Birthday value, EncoderContext encoderContext) {
            writer.writeDateTime(value.date.time)
        }

        @Override
        Class<Birthday> getEncoderClass() {
            return Birthday
        }
    }
}
