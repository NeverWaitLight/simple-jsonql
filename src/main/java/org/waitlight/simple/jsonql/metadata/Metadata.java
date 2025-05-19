package org.waitlight.simple.jsonql.metadata;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class Metadata {
    private final Map<String, PersistentClass> entities = new HashMap<>();

    public void addEntity(String entityName, PersistentClass persistentClass) {
        if (entities.containsKey(entityName)) {
            throw new RuntimeException("Duplicate entity name: '" + entityName + "'");
        }
        entities.put(entityName, persistentClass);
    }

    public PersistentClass getEntity(String entityName) {
        if (StringUtils.isBlank(entityName)) throw new RuntimeException("Entity not found");
        return entities.get(entityName.toLowerCase());
    }
}
