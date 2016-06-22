package org.grails.datastore.rx.rest.query

import com.damnhandy.uri.template.UriTemplate
import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import org.bson.Document
import org.bson.codecs.Codec
import org.bson.codecs.configuration.CodecRegistry
import org.grails.datastore.bson.json.JsonWriter
import org.grails.datastore.bson.query.BsonQuery
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.query.Query
import org.grails.datastore.rx.query.QueryState
import org.grails.datastore.rx.rest.RestEndpointPersistentEntity
import org.grails.datastore.rx.rest.RxRestDatastoreClient
import org.grails.datastore.rx.rest.codecs.RestEntityCodec
import org.springframework.util.LinkedMultiValueMap

/**
 * A query implementation that builds queries into a JSON object using the MongoDB query format. See https://docs.mongodb.com/manual/reference/operator/query/
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@CompileStatic
class BsonRxRestQuery<T> extends SimpleRxRestQuery<T> {

    final Set<String> validParameters

    BsonRxRestQuery(RxRestDatastoreClient client, PersistentEntity entity, QueryState queryState = new QueryState()) {
        super(client, entity, queryState)
        this.validParameters = client.defaultParameterNames
    }

    BsonRxRestQuery(RxRestDatastoreClient client, RestEndpointPersistentEntity entity, UriTemplate uriTemplate, QueryState queryState = new QueryState()) {
        super(client, entity, uriTemplate, queryState)
        this.validParameters = client.defaultParameterNames
    }

    @Override
    protected Set<String> calculateRemainingParameters(LinkedMultiValueMap<String, Object> queryParameters) {
        Set<String> remaining = super.calculateRemainingParameters(queryParameters)
        return remaining.findAll() { String name -> validParameters.contains(name)}
    }

    @Override
    protected LinkedMultiValueMap<String, Object> buildParameters(Map<String,Object> queryArguments) {
        LinkedMultiValueMap<String, Object> queryParameters = super.buildParameters(queryArguments)

        CodecRegistry registry = datastoreClient.codecRegistry
        Query.Junction junction = criteria
        if(junction instanceof Query.Conjunction && junction.criteria.size() == 1 && junction.criteria[0] instanceof Query.Junction) {
            junction = (Query.Junction)junction.criteria[0]
        }
        Document query = BsonQuery.createBsonQuery(registry, entity, junction )

        def queryString = new StringWriter()
        Codec codec = registry.get(Document)
        codec.encode(new JsonWriter(queryString), query, RestEntityCodec.DEFAULT_ENCODER_CONTEXT)
        queryParameters.add("q", queryString.toString())
        return queryParameters
    }
}
