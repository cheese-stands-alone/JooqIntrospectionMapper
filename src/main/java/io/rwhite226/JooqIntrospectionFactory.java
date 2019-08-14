package io.rwhite226;

import io.micronaut.context.annotation.Factory;
import org.jooq.RecordMapperProvider;
import org.jooq.RecordUnmapperProvider;

import javax.inject.Singleton;

@Factory
public class JooqIntrospectionFactory {

    @Singleton
    RecordMapperProvider buildRecordMapperProvider(JooqIntrospectionMapper instance) {
        return instance;
    }

    @Singleton
    RecordUnmapperProvider buildRecordUnmapperProvider(JooqIntrospectionUnMapper instance) {
        return instance;
    }

}
