package org.waitlight.simple.jsonql.metadata;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class MetadataSource {

    private final List<Class<?>> entityClasses = new ArrayList<>();

    public void addAnnotatedClass(Class<?> entityClass) {
        entityClasses.add(entityClass);
    }

}
