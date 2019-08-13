package io.rwhite226.unmappers;

import io.micronaut.core.beans.BeanProperty;
import io.rwhite226.Utils;
import org.jooq.*;
import org.jooq.exception.MappingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class PojoUnMapper<E, R extends Record> extends BaseUnMapper<E, R> {

    private static final Logger logger = LoggerFactory.getLogger(PojoUnMapper.class);

    protected final Map<Integer, BeanProperty<E, Object>> fieldIndexToProperty = new HashMap<>();

    public PojoUnMapper(
            Configuration configuration,
            RecordType<R> recordType,
            Class<E> type,
            RecordUnmapperProvider fallbackProvider
    ) {
        super(configuration, recordType, type, fallbackProvider);
        final Collection<BeanProperty<E, Object>> properties = introspection.getBeanProperties();
        final Map<String, BeanProperty<E, Object>> nameToProperty = new HashMap<>();
        for (final BeanProperty<E, Object> property : properties) {
            if (!property.isWriteOnly()) {
                final String name = Utils.getName(property);
                nameToProperty.put(name, property);
            }
        }
        for (int i = 0; i < fields.length; i++) {
            final Field<?> field = fields[i];
            final BeanProperty<E, Object> property = nameToProperty.get(field.getName().toLowerCase());
            if (property != null && field.getType().isAssignableFrom(property.getType())) {
                fieldIndexToProperty.put(i, property);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public R unmap(E source) throws MappingException {
        if (source == null) return null;
        try {
            Record record = newRecord();
            for (int i = 0; i < fields.length; i++) {
                final BeanProperty<E, Object> property = fieldIndexToProperty.get(i);
                if (property != null) {
                    final Field<Object> field = (Field<Object>) fields[i];
                    record.set(field, property.get(source));
                }
            }
            return (R) record;
        } catch (Exception e) {
            if (logger.isWarnEnabled())
                logger.warn("RecordUnMapper failed to instantiate" + type + " falling back to default", e);
            try {
                if (fallbackProvider != null) return fallbackProvider.provide(type, recordType).unmap(source);
                else throw new MappingException("An error ocurred when mapping record from " + type, e);
            } catch (Exception e2) {
                if (logger.isErrorEnabled()) logger.error("RecordUnMapper fallback also failed", e2);
                throw new MappingException("An error ocurred when mapping record from " + type, e);
            }
        }
    }
}
