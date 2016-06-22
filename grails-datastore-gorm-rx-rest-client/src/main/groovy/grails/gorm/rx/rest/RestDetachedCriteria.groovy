package grails.gorm.rx.rest

import grails.gorm.rx.DetachedCriteria
import grails.gorm.rx.rest.interceptor.ExistingClosureRequestBuilderInteceptor
import grails.gorm.rx.rest.interceptor.RequestBuilderInterceptor
import grails.http.client.builder.HttpClientRequestBuilder
import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import org.grails.datastore.gorm.query.criteria.AbstractDetachedCriteria
import org.grails.datastore.mapping.query.Query
import org.grails.datastore.mapping.query.api.ProjectionList
import org.grails.datastore.mapping.query.api.QueryableCriteria
import rx.Observable

/**
 * Extends the regular detached criteria and adds methods to customize the request
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@CompileStatic
@InheritConstructors
class RestDetachedCriteria<T> extends DetachedCriteria<T> {

    @Override
    Observable<T> find(Map args, @DelegatesTo(HttpClientRequestBuilder.class) Closure requestCustomizer) {
        args = new LinkedHashMap(args)
        args.interceptor = createInterceptor(requestCustomizer)
        return super.find(args, null)
    }

    @Override
    Observable<T> findAll(Map args, @DelegatesTo(HttpClientRequestBuilder.class) Closure requestCustomizer) {
        args = new LinkedHashMap(args)
        args.interceptor = createInterceptor(requestCustomizer)
        return super.findAll(args, null)
    }

    @Override
    Observable<T> get(Map args, @DelegatesTo(HttpClientRequestBuilder.class) Closure requestCustomizer) {
        args = new LinkedHashMap(args)
        args.interceptor = createInterceptor(requestCustomizer)
        return super.get(args, null)
    }

    @Override
    Observable<List<T>> toList(Map args, @DelegatesTo(HttpClientRequestBuilder.class) Closure requestCustomizer) {
        args = new LinkedHashMap(args)
        args.interceptor = createInterceptor(requestCustomizer)
        return super.toList(args, null)
    }

    @Override
    Observable<List<T>> list(Map args, @DelegatesTo(HttpClientRequestBuilder.class) Closure requestCustomizer) {
        args = new LinkedHashMap(args)
        args.interceptor = createInterceptor(requestCustomizer)
        return super.list(args, null)
    }

    @Override
    Observable<Number> getCount(Map args, @DelegatesTo(HttpClientRequestBuilder.class) Closure requestCustomizer) {
        args = new LinkedHashMap(args)
        args.interceptor = createInterceptor(requestCustomizer)
        return super.getCount(args, null)
    }

    @Override
    Observable<Number> count(Map args, @DelegatesTo(HttpClientRequestBuilder.class) Closure requestCustomizer) {
        args = new LinkedHashMap(args)
        args.interceptor = createInterceptor(requestCustomizer)
        return super.count(args, null)
    }

    @Override
    Observable<T> get(@DelegatesTo(HttpClientRequestBuilder.class) Closure requestCustomizer) {
        return get([:], requestCustomizer)
    }

    Observable<List<T>> list(@DelegatesTo(HttpClientRequestBuilder.class) Closure requestCustomizer) {
        return list([:], requestCustomizer)
    }


    Observable<T> find(@DelegatesTo(HttpClientRequestBuilder.class) Closure requestCustomizer) {
        return find([:], requestCustomizer)
    }

    Observable<T> findAll(@DelegatesTo(HttpClientRequestBuilder.class) Closure requestCustomizer) {
        return findAll([:], requestCustomizer)
    }

    Observable<Number> count(@DelegatesTo(HttpClientRequestBuilder.class) Closure requestCustomizer) {
        return count([:], requestCustomizer)
    }

    @Override
    DetachedCriteria<T> where(@DelegatesTo(AbstractDetachedCriteria) Closure additionalQuery) {
        return (DetachedCriteria) super.where(additionalQuery)
    }

    @Override
    RestDetachedCriteria<T> whereLazy(@DelegatesTo(AbstractDetachedCriteria) Closure additionalQuery) {
        return (RestDetachedCriteria) super.whereLazy(additionalQuery)
    }

    @Override
    RestDetachedCriteria<T> build(@DelegatesTo(grails.gorm.DetachedCriteria) Closure callable) {
        return (RestDetachedCriteria)super.build(callable)
    }

    @Override
    RestDetachedCriteria<T> buildLazy(@DelegatesTo(grails.gorm.DetachedCriteria) Closure callable) {
        return (RestDetachedCriteria)super.buildLazy(callable)
    }

    @Override
    RestDetachedCriteria<T> max(int max) {
        return (RestDetachedCriteria)super.max(max)
    }

    @Override
    RestDetachedCriteria<T> offset(int offset) {
        return (RestDetachedCriteria)super.offset(offset)
    }

    @Override
    RestDetachedCriteria<T> sort(String property) {
        return (RestDetachedCriteria)super.sort(property)
    }

    @Override
    RestDetachedCriteria<T> sort(String property, String direction) {
        return (RestDetachedCriteria)super.sort(property, direction)
    }

    @Override
    RestDetachedCriteria<T> property(String property) {
        return (RestDetachedCriteria)super.property(property)
    }

    @Override
    RestDetachedCriteria<T> id() {
        return (RestDetachedCriteria)super.id()
    }

    @Override
    RestDetachedCriteria<T> avg(String property) {
        return (RestDetachedCriteria)super.avg(property)
    }

    @Override
    RestDetachedCriteria<T> sum(String property) {
        return (RestDetachedCriteria)super.sum(property)
    }

    @Override
    RestDetachedCriteria<T> min(String property) {
        return (RestDetachedCriteria)super.min(property)
    }

    @Override
    RestDetachedCriteria<T> max(String property) {
        return (RestDetachedCriteria)super.max(property)
    }

    @Override
    RestDetachedCriteria<T> distinct(String property) {
        return (RestDetachedCriteria)super.distinct(property)
    }

    @Override
    protected RestDetachedCriteria<T> clone() {
        return (RestDetachedCriteria)super.clone()
    }

    @Override
    protected RestDetachedCriteria newInstance() {
        new RestDetachedCriteria(targetClass, alias)
    }

    @Override
    RestDetachedCriteria<T> join(String property) {
        return (RestDetachedCriteria<T>)super.join(property)
    }

    @Override
    DetachedCriteria<T> select(String property) {
        return (DetachedCriteria<T>)super.select(property)
    }

    @Override
    RestDetachedCriteria<T> projections(@DelegatesTo(ProjectionList) Closure callable) {
        return (RestDetachedCriteria<T>)super.projections(callable)
    }

    @Override
    RestDetachedCriteria<T> and(@DelegatesTo(AbstractDetachedCriteria) Closure callable) {
        return (RestDetachedCriteria<T>)super.and(callable)
    }

    @Override
    RestDetachedCriteria<T> or(@DelegatesTo(AbstractDetachedCriteria) Closure callable) {
        return (RestDetachedCriteria<T>)super.or(callable)
    }

    @Override
    DetachedCriteria<T> not(@DelegatesTo(AbstractDetachedCriteria) Closure callable) {
        return (DetachedCriteria<T>)super.not(callable)
    }

    @Override
    RestDetachedCriteria<T> "in"(String propertyName, Collection values) {
        return (RestDetachedCriteria<T>)super.in(propertyName, values)
    }

    @Override
    RestDetachedCriteria<T> "in"(String propertyName, QueryableCriteria subquery) {
        return (RestDetachedCriteria<T>)super.in(propertyName, subquery)
    }

    @Override
    DetachedCriteria<T> inList(String propertyName, QueryableCriteria<?> subquery) {
        return (DetachedCriteria<T>)super.inList(propertyName, subquery)
    }

    @Override
    DetachedCriteria<T> "in"(String propertyName, @DelegatesTo(AbstractDetachedCriteria) Closure<?> subquery) {
        return (DetachedCriteria<T>)super.in(propertyName, subquery)
    }

    @Override
    RestDetachedCriteria<T> inList(String propertyName, @DelegatesTo(AbstractDetachedCriteria) Closure<?> subquery) {
        return (RestDetachedCriteria<T>)super.inList(propertyName, subquery)
    }

    @Override
    RestDetachedCriteria<T> "in"(String propertyName, Object[] values) {
        return (RestDetachedCriteria<T>)super.in(propertyName, values)
    }

    @Override
    RestDetachedCriteria<T> notIn(String propertyName, QueryableCriteria<?> subquery) {
        return (RestDetachedCriteria<T>)super.notIn(propertyName, subquery)
    }

    @Override
    RestDetachedCriteria<T> notIn(String propertyName, @DelegatesTo(AbstractDetachedCriteria) Closure<?> subquery) {
        return (RestDetachedCriteria<T>)super.notIn(propertyName, subquery)
    }

    @Override
    RestDetachedCriteria<T> order(String propertyName) {
        return (RestDetachedCriteria<T>)super.order(propertyName)
    }

    @Override
    RestDetachedCriteria<T> order(Query.Order o) {
        return (RestDetachedCriteria<T>)super.order(o)
    }

    @Override
    RestDetachedCriteria<T> order(String propertyName, String direction) {
        return (RestDetachedCriteria<T>)super.order(propertyName, direction)
    }

    @Override
    RestDetachedCriteria<T> inList(String propertyName, Collection values) {
        return (RestDetachedCriteria<T>)super.inList(propertyName, values)
    }


    @Override
    RestDetachedCriteria<T> inList(String propertyName, Object[] values) {
        return (RestDetachedCriteria<T>)super.inList(propertyName, values)
    }

    @Override
    RestDetachedCriteria<T> sizeEq(String propertyName, int size) {
        return (RestDetachedCriteria<T>)super.sizeEq(propertyName, size)
    }

    @Override
    RestDetachedCriteria<T> sizeGt(String propertyName, int size) {
        return (RestDetachedCriteria<T>)super.sizeGt(propertyName, size)
    }

    @Override
    RestDetachedCriteria<T> sizeGe(String propertyName, int size) {
        return (RestDetachedCriteria<T>)super.sizeGe(propertyName, size)
    }

    @Override
    RestDetachedCriteria<T> sizeLe(String propertyName, int size) {
        return (RestDetachedCriteria<T>)super.sizeLe(propertyName, size)
    }

    @Override
    RestDetachedCriteria<T> sizeLt(String propertyName, int size) {
        return (RestDetachedCriteria<T>)super.sizeLt(propertyName, size)
    }

    @Override
    RestDetachedCriteria<T> sizeNe(String propertyName, int size) {
        return (RestDetachedCriteria<T>)super.sizeNe(propertyName, size)
    }

    @Override
    DetachedCriteria<T> eqProperty(String propertyName, String otherPropertyName) {
        return (DetachedCriteria<T>)super.eqProperty(propertyName, otherPropertyName)
    }

    @Override
    RestDetachedCriteria<T> neProperty(String propertyName, String otherPropertyName) {
        return (RestDetachedCriteria<T>)super.neProperty(propertyName, otherPropertyName)
    }

    @Override
    RestDetachedCriteria<T> allEq(Map<String, Object> propertyValues) {
        return (RestDetachedCriteria<T>)super.allEq(propertyValues)
    }

    @Override
    RestDetachedCriteria<T> gtProperty(String propertyName, String otherPropertyName) {
        return (RestDetachedCriteria<T>)super.gtProperty(propertyName, otherPropertyName)
    }

    @Override
    DetachedCriteria<T> geProperty(String propertyName, String otherPropertyName) {
        return (DetachedCriteria<T>)super.geProperty(propertyName, otherPropertyName)
    }

    @Override
    RestDetachedCriteria<T> ltProperty(String propertyName, String otherPropertyName) {
        return (RestDetachedCriteria<T>)super.ltProperty(propertyName, otherPropertyName)
    }

    @Override
    RestDetachedCriteria<T> leProperty(String propertyName, String otherPropertyName) {
        return (RestDetachedCriteria<T>)super.leProperty(propertyName, otherPropertyName)
    }

    @Override
    RestDetachedCriteria<T> idEquals(Object value) {
        return (RestDetachedCriteria<T>)super.idEquals(value)
    }

    @Override
    RestDetachedCriteria<T> exists(QueryableCriteria<?> subquery) {
        return (RestDetachedCriteria<T>)super.exists(subquery)
    }

    @Override
    RestDetachedCriteria<T> notExists(QueryableCriteria<?> subquery) {
        return (RestDetachedCriteria<T>)super.notExists(subquery)
    }

    @Override
    RestDetachedCriteria<T> isEmpty(String propertyName) {
        return (RestDetachedCriteria<T>)super.isEmpty(propertyName)
    }

    @Override
    RestDetachedCriteria<T> isNotEmpty(String propertyName) {
        return (RestDetachedCriteria<T>)super.isNotEmpty(propertyName)
    }

    @Override
    RestDetachedCriteria<T> isNull(String propertyName) {
        return (RestDetachedCriteria<T>)super.isNull(propertyName)
    }

    @Override
    RestDetachedCriteria<T> isNotNull(String propertyName) {
        return (RestDetachedCriteria<T>)super.isNotNull(propertyName)
    }

    @Override
    RestDetachedCriteria<T> eq(String propertyName, Object propertyValue) {
        return (RestDetachedCriteria<T>)super.eq(propertyName, propertyValue)
    }

    @Override
    RestDetachedCriteria<T> idEq(Object propertyValue) {
        return (RestDetachedCriteria<T>)super.idEq(propertyValue)
    }

    @Override
    RestDetachedCriteria<T> ne(String propertyName, Object propertyValue) {
        return (RestDetachedCriteria<T>)super.ne(propertyName, propertyValue)
    }

    @Override
    RestDetachedCriteria<T> between(String propertyName, Object start, Object finish) {
        return (RestDetachedCriteria<T>)super.between(propertyName, start, finish)
    }

    @Override
    RestDetachedCriteria<T> gte(String property, Object value) {
        return (RestDetachedCriteria<T>)super.gte(property, value)
    }

    @Override
    RestDetachedCriteria<T> ge(String property, Object value) {
        return (RestDetachedCriteria<T>)super.ge(property, value)
    }

    @Override
    RestDetachedCriteria<T> gt(String property, Object value) {
        return (RestDetachedCriteria<T>)super.gt(property, value)
    }

    @Override
    RestDetachedCriteria<T> lte(String property, Object value) {
        return (RestDetachedCriteria<T>)super.lte(property, value)
    }

    @Override
    RestDetachedCriteria<T> le(String property, Object value) {
        return (RestDetachedCriteria<T>)super.le(property, value)
    }

    @Override
    RestDetachedCriteria<T> lt(String property, Object value) {
        return (RestDetachedCriteria<T>)super.lt(property, value)
    }

    @Override
    RestDetachedCriteria<T> like(String propertyName, Object propertyValue) {
        return (RestDetachedCriteria<T>)super.like(propertyName, propertyValue)
    }

    @Override
    RestDetachedCriteria<T> ilike(String propertyName, Object propertyValue) {
        return (RestDetachedCriteria<T>)super.ilike(propertyName, propertyValue)
    }

    @Override
    RestDetachedCriteria<T> rlike(String propertyName, Object propertyValue) {
        return (RestDetachedCriteria<T>)super.rlike(propertyName, propertyValue)
    }

    @Override
    RestDetachedCriteria<T> eqAll(String propertyName, @DelegatesTo(AbstractDetachedCriteria) Closure<?> propertyValue) {
        return (RestDetachedCriteria<T>)super.eqAll(propertyName, propertyValue)
    }

    @Override
    RestDetachedCriteria<T> gtAll(String propertyName, @DelegatesTo(AbstractDetachedCriteria) Closure<?> propertyValue) {
        return (RestDetachedCriteria<T>)super.gtAll(propertyName, propertyValue)
    }

    @Override
    RestDetachedCriteria<T> ltAll(String propertyName, @DelegatesTo(AbstractDetachedCriteria) Closure<?> propertyValue) {
        return (RestDetachedCriteria<T>)super.ltAll(propertyName, propertyValue)
    }

    @Override
    RestDetachedCriteria<T> geAll(String propertyName, @DelegatesTo(AbstractDetachedCriteria) Closure<?> propertyValue) {
        return (RestDetachedCriteria<T>)super.geAll(propertyName, propertyValue)
    }

    @Override
    RestDetachedCriteria<T> leAll(String propertyName, @DelegatesTo(AbstractDetachedCriteria) Closure<?> propertyValue) {
        return (RestDetachedCriteria<T>)super.leAll(propertyName, propertyValue)
    }

    @Override
    RestDetachedCriteria<T> eqAll(String propertyName, QueryableCriteria propertyValue) {
        return (RestDetachedCriteria<T>)super.eqAll(propertyName, propertyValue)
    }

    @Override
    RestDetachedCriteria<T> gtAll(String propertyName, QueryableCriteria propertyValue) {
        return (RestDetachedCriteria<T>)super.gtAll(propertyName, propertyValue)
    }

    @Override
    RestDetachedCriteria<T> gtSome(String propertyName, QueryableCriteria propertyValue) {
        return (RestDetachedCriteria<T>)super.gtSome(propertyName, propertyValue)
    }

    @Override
    RestDetachedCriteria<T> gtSome(String propertyName, @DelegatesTo(AbstractDetachedCriteria) Closure<?> propertyValue) {
        return (RestDetachedCriteria<T>)super.gtSome(propertyName, propertyValue)
    }

    @Override
    DetachedCriteria<T> geSome(String propertyName, QueryableCriteria propertyValue) {
        return (DetachedCriteria<T>)super.geSome(propertyName, propertyValue)
    }

    @Override
    RestDetachedCriteria<T> geSome(String propertyName, @DelegatesTo(AbstractDetachedCriteria) Closure<?> propertyValue) {
        return (RestDetachedCriteria<T>)super.geSome(propertyName, propertyValue)
    }

    @Override
    RestDetachedCriteria<T> ltSome(String propertyName, QueryableCriteria propertyValue) {
        return (RestDetachedCriteria<T>)super.ltSome(propertyName, propertyValue)
    }

    @Override
    RestDetachedCriteria<T> ltSome(String propertyName, @DelegatesTo(AbstractDetachedCriteria) Closure<?> propertyValue) {
        return (RestDetachedCriteria<T>)super.ltSome(propertyName, propertyValue)
    }

    @Override
    RestDetachedCriteria<T> leSome(String propertyName, QueryableCriteria propertyValue) {
        return (RestDetachedCriteria<T>)super.leSome(propertyName, propertyValue)
    }

    @Override
    RestDetachedCriteria<T> leSome(String propertyName, @DelegatesTo(AbstractDetachedCriteria) Closure<?> propertyValue) {
        return (RestDetachedCriteria<T>)super.leSome(propertyName, propertyValue)
    }

    @Override
    RestDetachedCriteria<T> ltAll(String propertyName, QueryableCriteria propertyValue) {
        return (RestDetachedCriteria<T>)super.ltAll(propertyName, propertyValue)
    }

    @Override
    RestDetachedCriteria<T> geAll(String propertyName, QueryableCriteria propertyValue) {
        return (RestDetachedCriteria<T>)super.geAll(propertyName, propertyValue)
    }

    @Override
    RestDetachedCriteria<T> leAll(String propertyName, QueryableCriteria propertyValue) {
        return (RestDetachedCriteria<T>)super.leAll(propertyName, propertyValue)
    }

    protected RequestBuilderInterceptor createInterceptor(Closure callable) {
        if(callable != null) {
            def inteceptor = new ExistingClosureRequestBuilderInteceptor(callable)
            return inteceptor
        }
        return null
    }
}
