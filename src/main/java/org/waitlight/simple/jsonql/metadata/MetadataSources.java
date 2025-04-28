package org.waitlight.simple.jsonql.metadata;

import java.util.ArrayList;
import java.util.List;

public class MetadataSources {

    private final List<Class<?>> entityClasses = new ArrayList<>();

    public void addAnnotatedClass(Class<?> entityClass) {
        entityClasses.add(entityClass);
    }

    public Metadata buildMetadata() {
        Metadata metadata = new Metadata();
        AnnotationBinder binder = new AnnotationBinder();
        for (Class<?> entityClass : entityClasses) {
            PersistentClass persistentClass = binder.bindEntity(entityClass);
            metadata.addEntityBinding(entityClass.getSimpleName().toLowerCase(), persistentClass);
        }
        return metadata;
    }
}
