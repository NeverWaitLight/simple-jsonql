package org.waitlight.simple.jsonql.metadata;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class DefaultMetadataCache implements MetadataCache<PersistentClass, PersistentClass> {

    private final Map<String, PersistentClass> entities = new HashMap<>();

    @Override
    public void add(PersistentClass persistentClass) {
        String entityName = persistentClass.getEntityName();
        entityName = StringUtils.uncapitalize(entityName);

        if (entities.containsKey(entityName)) {
            throw new MetadataException("Duplicate entity name: '" + entityName + "'");
        }
        entities.put(entityName, persistentClass);
    }

    @Override
    public PersistentClass get(String entityName) {
        if (StringUtils.isBlank(entityName)) {
            throw new MetadataException("Entity [null] not found");
        }

        PersistentClass entity = entities.get(entityName);
        if (entity == null) {
            entity = entities.get(entityName.toLowerCase());
        }
        if (entity == null) {
            throw new MetadataException("Entity [%s] not found", entityName);
        }
        return entity;
    }
}