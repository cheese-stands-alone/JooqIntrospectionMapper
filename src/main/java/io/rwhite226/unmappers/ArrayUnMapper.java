package io.rwhite226.unmappers;

import org.jooq.*;
import org.jooq.exception.MappingException;

public class ArrayUnMapper<E, R extends Record> extends BaseUnMapper<E, R> {

    public ArrayUnMapper(
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
        if (source instanceof Object[]) {
            final Object[] array = (Object[]) source;
            final int size = recordType.size();
            final Record record = newRecord();
            for (int i = 0; i < size && i < array.length; i++) {
                final Field field = recordType.field(i);
                if (recordType.field(field) != null) record.setValue(field, array[i]);
            }

            return (R) record;
        }
        throw new MappingException("Object[] expected. Got: " + source.getClass());
    }
}
