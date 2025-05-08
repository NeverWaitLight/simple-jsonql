package org.waitlight.simple.jsonql.metadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MetadataSources {

    private final List<Class<?>> entityClasses = new ArrayList<>();
    private Metadata metadata;

    public void addAnnotatedClass(Class<?> entityClass) {
        entityClasses.add(entityClass);
    }

    public Metadata buildMetadata() {
        if (Objects.nonNull(metadata)) {
            return metadata;
        }

        metadata = new Metadata();
        EntityMetadataBuilder binder = new EntityMetadataBuilder();
        for (Class<?> entityClass : entityClasses) {
            PersistentClass persistentClass = binder.bindEntity(entityClass);
            metadata.addEntityBinding(entityClass.getSimpleName().toLowerCase(), persistentClass);
        }
        return metadata;
    }
}
