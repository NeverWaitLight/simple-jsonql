package org.waitlight.simple.jsonql.metadata;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class Metadata {
    private final Map<String, PersistentClass> entityBindings = new HashMap<>();

    public void addEntityBinding(String entityName, PersistentClass persistentClass) {
        entityBindings.put(entityName, persistentClass);
    }

    public PersistentClass getEntityBinding(String entityName) {
        if (StringUtils.isBlank(entityName)) throw new RuntimeException("Entity not found");
        return entityBindings.get(entityName.toLowerCase());
    }
}
