package io.rwhite226;

import io.micronaut.core.beans.BeanIntrospection;
import io.micronaut.core.beans.BeanIntrospector;
import io.micronaut.core.beans.BeanProperty;
import io.micronaut.core.type.Argument;
import org.jooq.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

public class JooqIntrospectionMapper implements RecordMapperProvider {

    private static final Logger logger = LoggerFactory.getLogger(JooqIntrospectionMapper.class);

    protected static final Pattern regex = Pattern.compile("[-_\\s.]");

    // Map of cached RecordMappers
    protected static final ConcurrentMap<Pair, RecordMapper<?, ?>> cache = new ConcurrentHashMap<>();

    protected final RecordMapperProvider fallbackProvider;
    protected final Configuration configuration;

    /**
     * Pair class to hold the recordType and type to use as compound keys to lookup the cached RecordMapper
     */
    protected static class Pair {
        final RecordType<?> recordType;
        final Class<?> type;

        Pair(RecordType<?> recordType, Class<?> type) {
            this.recordType = recordType;
            this.type = type;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Pair) {
                final Pair pairObj = (Pair) obj;
                return recordType.equals(pairObj.recordType) && type.equals(pairObj.type);
            } else return false;
        }

        @Override
        public int hashCode() {
            return 31 * recordType.hashCode() + type.hashCode();
        }
    }

    JooqIntrospectionMapper(Configuration configuration, RecordMapperProvider fallbackProvider) {
        Objects.requireNonNull(configuration);
        this.fallbackProvider = fallbackProvider;
        this.configuration = configuration;
    }

    JooqIntrospectionMapper(Configuration configuration) {
        this(configuration, null);
    }

    protected Pattern getRegex() {
        return regex;
    }

    @Override
    public <R extends Record, E> RecordMapper<R, E> provide(RecordType<R> recordType, Class<? extends E> type) {
        @SuppressWarnings("unchecked") final Class<E> castedType = (Class<E>) type;
        // Cache the RecordMapper if we are allowed otherwise build one for each call
        if (Boolean.TRUE.equals(configuration.settings().isCacheRecordMappers())) {
            @SuppressWarnings("unchecked") final RecordMapper<R, E> cashed = (RecordMapper<R, E>) cache.computeIfAbsent(
                    new Pair(recordType, castedType),
                    (it) -> build(recordType, castedType)
            );
            return cashed;
        } else return build(recordType, castedType);
    }

    protected <R extends Record, E> RecordMapper<R, E> build(RecordType<R> recordType, Class<E> type) {
        final Field<?>[] fields = recordType.fields();
        final BeanIntrospection<E> introspection = BeanIntrospector.SHARED.getIntrospection(type);
        final Argument<?>[] constructorArguments = introspection.getConstructorArguments();
        final Collection<BeanProperty<E, Object>> beanProperties = introspection.getBeanProperties();
        // Maps the fields indexes to BeanProperty used for setting the property
        final Map<Integer, BeanProperty<E, Object>> propertyMap = new HashMap<>();
        // Maps the fields indexes to the indexes on the beans constructor
        final int[] constructorIndexes = new int[constructorArguments.length];
        fieldsLoop:
        for (int fieldIndex = 0; fieldIndex < fields.length; fieldIndex++) {
            final Field<?> field = fields[fieldIndex];

            // Normalize the name removing spaces, dashed, underscores, etc
            final String normalized = getRegex().matcher(field.getName()).replaceAll("");

            // Check to see if the field is part of the bean's constructor
            for (int i = 0; i < constructorArguments.length; i++) {
                final Argument<?> argument = constructorArguments[i];
                final String name = argument.getName();
                if ((name.equalsIgnoreCase(field.getName()) || name.equalsIgnoreCase(normalized)) &&
                        argument.getType().isAssignableFrom(field.getType())
                ) {
                    constructorIndexes[i] = fieldIndex;
                    continue fieldsLoop;
                }
            }
            // Otherwise check to see if the field is on the of bean's settable properties
            for (BeanProperty<E, Object> property : beanProperties) {
                final String name = property.getName();
                if (!property.isReadOnly() && property.getType().isAssignableFrom(field.getType()) &&
                        (name.equalsIgnoreCase(field.getName()) || name.equalsIgnoreCase(normalized))
                ) {
                    propertyMap.put(fieldIndex, property);
                    continue fieldsLoop;
                }
            }
        }
        return record -> {
            try {
                final E bean;
                if (constructorIndexes.length > 0) {
                    final Object[] args = new Object[constructorArguments.length];
                    for (int i = 0; i < constructorArguments.length; i++) {
                        int fieldIndex = constructorIndexes[i];
                        if (fieldIndex >= 0) {
                            final Field<?> field = fields[fieldIndex];
                            if (field != null) args[i] = field.getValue(record);
                        }
                    }
                    bean = introspection.instantiate(args);
                } else bean = introspection.instantiate();
                for (Map.Entry<Integer, BeanProperty<E, Object>> entry : propertyMap.entrySet()) {
                    final Field<?> field = fields[entry.getKey()];
                    if (field != null) entry.getValue().set(bean, field.getValue(record));
                }
                return bean;
            } catch (Exception e) {
                if (logger.isWarnEnabled())
                    logger.warn("RecordMapper failed to instantiate" + type + " falling back to default", e);
                try {
                    if (fallbackProvider != null) return fallbackProvider.provide(recordType, type).map(record);
                    else throw e;
                } catch (Exception fallbackException) {
                    if (logger.isErrorEnabled()) logger.error("RecordMapper fallback also failed", fallbackException);
                    throw e;
                }
            }
        };
    }
}
