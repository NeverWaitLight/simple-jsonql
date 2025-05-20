package org.waitlight.simple.jsonql.metadata;

import lombok.Getter;

import java.util.HashSet;
import java.util.Set;

@Getter
public class MetadataSource {

    private final Set<Class<?>> entityClasses = new HashSet<>();

    public void registry(Class<?> entityClass) {
        entityClasses.add(entityClass);
    }

}
