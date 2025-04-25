package org.waitlight.simple.jsonql.metadata;

import java.util.HashMap;
import java.util.Map;

public class Metadata {
    private final Map<String, PersistentClass> entityBindings = new HashMap<>();

    public void addEntityBinding(String entityName, PersistentClass persistentClass) {
        entityBindings.put(entityName, persistentClass);
    }

    public PersistentClass getEntityBinding(String entityName) {
        return entityBindings.get(entityName);
    }
}
