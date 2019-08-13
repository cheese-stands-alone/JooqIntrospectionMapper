package io.rwhite226.unmappers;

import io.micronaut.core.beans.BeanIntrospection;
import io.micronaut.core.beans.BeanIntrospector;
import org.jooq.*;
import org.jooq.impl.DSL;

public abstract class BaseUnMapper<E, R extends Record> implements RecordUnmapper<E, R> {

    protected final RecordType<R> recordType;
    protected final Configuration configuration;
    protected final BeanIntrospection<E> introspection;
    protected final Class<E> type;
    protected final Field<?>[] fields;
    protected final RecordUnmapperProvider fallbackProvider;

    BaseUnMapper(
            Configuration configuration,
            RecordType<R> recordType,
            Class<E> type,
            RecordUnmapperProvider fallbackProvider
    ) {
        this.configuration = configuration;
        this.recordType = recordType;
        this.type = type;
        this.introspection = BeanIntrospector.SHARED.getIntrospection(type);
        this.fields = recordType.fields();
        this.fallbackProvider = fallbackProvider;
    }

    protected Record newRecord() {
        return DSL.using(configuration).newRecord(recordType.fields());
    }

}
