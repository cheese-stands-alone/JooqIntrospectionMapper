package io.rwhite226;

import io.micronaut.core.beans.BeanProperty;

public class Utils {
    public static String getName(BeanProperty<?, ?> argument) {
        return argument.findAnnotation("javax.persistence.Column")
                .flatMap(it -> it.get("name", String.class))
                .orElseGet(argument::getName).toLowerCase();
    }
}
