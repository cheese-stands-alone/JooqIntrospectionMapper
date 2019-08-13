package io.rwhite226.unmappers;

import org.jooq.*;
import org.jooq.exception.MappingException;

import java.util.Map;

public class MapUnMapper<E, R extends Record> extends BaseUnMapper<E, R> {

    public MapUnMapper(
            Configuration configuration,
            RecordType<R> recordType,
            Class<E> type,
            RecordUnmapperProvider fallbackProvider
    ) {
        super(configuration, recordType, type, fallbackProvider);
    }

    @Override
    @SuppressWarnings("unchecked")
    public final R unmap(E source) {
        if (source == null) return null;

        if (source instanceof Map) {
            final Map<?, ?> map = (Map<?, ?>) source;
            final Record record = newRecord();
            for (final Field field : fields) {
                final String name = field.getName();
                if (map.containsKey(name)) record.setValue(field, map.get(name));
            }
            return (R) record;
        }

        throw new MappingException("Map expected. Got: " + source.getClass());
    }
}