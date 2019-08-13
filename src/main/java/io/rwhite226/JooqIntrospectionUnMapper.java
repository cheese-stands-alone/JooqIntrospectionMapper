package io.rwhite226;

import io.rwhite226.unmappers.ArrayUnMapper;
import io.rwhite226.unmappers.MapUnMapper;
import io.rwhite226.unmappers.PojoUnMapper;
import org.jooq.*;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class JooqIntrospectionUnMapper implements RecordUnmapperProvider {

    // Map of cached RecordMappers
    protected static final ConcurrentMap<Pair, RecordUnmapper<?, ?>> cache = new ConcurrentHashMap<>();


    protected final Configuration configuration;
    protected final RecordUnmapperProvider fallbackProvider;

    public JooqIntrospectionUnMapper(Configuration configuration, RecordUnmapperProvider fallbackProvider) {
        Objects.requireNonNull(configuration);
        this.configuration = configuration;
        this.fallbackProvider = fallbackProvider;
    }

    @Override
    public <E, R extends Record> RecordUnmapper<E, R> provide(Class<? extends E> type, RecordType<R> recordType) {
        @SuppressWarnings("unchecked") final Class<E> castedType = (Class<E>) type;
        // Cache the RecordMapper if we are allowed otherwise build one for each call
        if (Boolean.TRUE.equals(configuration.settings().isCacheRecordMappers())) {
            @SuppressWarnings("unchecked") final RecordUnmapper<E, R> cashed = (RecordUnmapper<E, R>) cache.computeIfAbsent(
                    new Pair(recordType, castedType),
                    (it) -> build(recordType, castedType)
            );
            return cashed;
        } else return build(recordType, castedType);
    }

    protected <E, R extends Record> RecordUnmapper<E, R> build(RecordType<R> recordType, Class<E> type) {
        if (type.isArray()) return new ArrayUnMapper<>(configuration, recordType, type, fallbackProvider);
        else if (Map.class.isAssignableFrom(type))
            return new MapUnMapper<>(configuration, recordType, type, fallbackProvider);
        else return new PojoUnMapper<>(configuration, recordType, type, fallbackProvider);
    }

}
