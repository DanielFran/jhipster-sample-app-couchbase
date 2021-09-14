package io.github.jhipster.sample.config.couchbase;

import static org.springframework.data.couchbase.core.query.N1QLExpression.*;

import java.util.Iterator;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.couchbase.core.CouchbaseOperations;
import org.springframework.data.couchbase.core.convert.CouchbaseConverter;
import org.springframework.data.couchbase.core.mapping.CouchbasePersistentProperty;
import org.springframework.data.couchbase.core.query.Query;
import org.springframework.data.couchbase.core.query.QueryCriteria;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.PersistentPropertyPath;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.parser.AbstractQueryCreator;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;

public class CustomN1qlQueryCreator extends AbstractQueryCreator<Query, QueryCriteria> {

    private final CouchbaseOperations couchbaseOperations;
    private final ParameterAccessor accessor;
    private final MappingContext<?, CouchbasePersistentProperty> context;
    private final QueryMethod queryMethod;
    private final CouchbaseConverter converter;

    public CustomN1qlQueryCreator(
        final CouchbaseOperations operations,
        final PartTree tree,
        final ParameterAccessor accessor,
        QueryMethod queryMethod,
        CouchbaseConverter converter
    ) {
        super(tree, accessor);
        this.accessor = accessor;
        this.context = converter.getMappingContext();
        this.queryMethod = queryMethod;
        this.converter = converter;
        this.couchbaseOperations = operations;
    }

    static Converter<? super CouchbasePersistentProperty, String> cvtr = source ->
        new StringBuilder(source.getFieldName().length() + 2).append('`').append(source.getFieldName()).append('`').toString();

    @Override
    protected QueryCriteria create(final Part part, final Iterator<Object> iterator) {
        PersistentPropertyPath<CouchbasePersistentProperty> path = context.getPersistentPropertyPath(part.getProperty());
        CouchbasePersistentProperty property = path.getLeafProperty();
        return from(part, property, QueryCriteria.where(addMetaIfIdProperty(path, property)), iterator);
    }

    @Override
    protected QueryCriteria and(Part part, QueryCriteria base, Iterator<Object> iterator) {
        if (base == null) {
            return create(part, iterator);
        }
        PersistentPropertyPath<CouchbasePersistentProperty> path = context.getPersistentPropertyPath(part.getProperty());
        CouchbasePersistentProperty property = path.getLeafProperty();
        return from(part, property, base.and(addMetaIfIdProperty(path, property)), iterator);
    }

    private String addMetaIfIdProperty(
        final PersistentPropertyPath<CouchbasePersistentProperty> path,
        final CouchbasePersistentProperty property
    ) {
        return path.toDotPath(cvtr);
    }

    @Override
    protected QueryCriteria or(QueryCriteria base, QueryCriteria criteria) {
        return base.or(criteria);
    }

    @Override
    protected Query complete(QueryCriteria criteria, Sort sort) {
        return (criteria == null ? new Query() : new Query().addCriteria(criteria)).with(sort);
    }

    private QueryCriteria from(
        final Part part,
        final CouchbasePersistentProperty property,
        final QueryCriteria criteria,
        final Iterator<Object> parameters
    ) {
        final Part.Type type = part.getType();

        switch (type) {
            case GREATER_THAN:
            case AFTER:
                return criteria.gt(parameters.next());
            case GREATER_THAN_EQUAL:
                return criteria.gte(parameters.next());
            case LESS_THAN:
            case BEFORE:
                return criteria.lt(parameters.next());
            case LESS_THAN_EQUAL:
                return criteria.lte(parameters.next());
            case SIMPLE_PROPERTY:
                return criteria.eq(parameters.next());
            case NEGATING_SIMPLE_PROPERTY:
                return criteria.ne(parameters.next());
            case CONTAINING:
                return criteria.containing(parameters.next());
            case NOT_CONTAINING:
                return criteria.notContaining(parameters.next());
            case STARTING_WITH:
                return criteria.startingWith(parameters.next());
            case ENDING_WITH:
                return criteria.endingWith(parameters.next());
            case LIKE:
                return criteria.like(parameters.next());
            case NOT_LIKE:
                return criteria.notLike(parameters.next());
            case WITHIN:
                return criteria.within(parameters.next());
            case IS_NULL:
                return criteria.isNull();
            case IS_NOT_NULL:
                return criteria.isNotNull();
            case IS_EMPTY:
                return criteria.isNotValued();
            case IS_NOT_EMPTY:
                return criteria.isValued();
            case EXISTS:
                return criteria.isNotMissing();
            case REGEX:
                return criteria.regex(parameters.next());
            case BETWEEN:
                return criteria.between(parameters.next(), parameters.next());
            case IN:
                return criteria.in(parameters.next());
            case NOT_IN:
                return criteria.notIn(parameters.next());
            case TRUE:
                return criteria.TRUE();
            case FALSE:
                return criteria.FALSE();
            default:
                throw new IllegalArgumentException("Unsupported keyword!");
        }
    }
}
