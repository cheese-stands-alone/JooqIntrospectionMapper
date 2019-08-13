package io.rwhite226;

import org.jooq.RecordType;

/**
 * Pair class to hold the recordType and type to use as compound keys to lookup the cached mappers/unamappers
 */
public class Pair {
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
