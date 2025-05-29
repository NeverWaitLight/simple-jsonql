package org.waitlight.simple.jsonql.metadata;

import lombok.Getter;
import lombok.Setter;
import org.waitlight.simple.jsonql.builder.SqlBuildException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class PersistentClass {
    private Class<?> entityClass;
    private String entityName;

    private String tableName;

    private List<Property> properties = new ArrayList<>();

    // 与当前类有应用关系的其他类
    private Map<Class<?>, RelationshipType> relations = new HashMap<>();

    public PersistentClass(Class<?> entityClazz, String entityName) {
        this.entityClass = entityClazz;
        this.entityName = entityName;
    }

    public void addProperty(Property property) {
        properties.add(property);
    }

    public void addRelationProperty(Class<?> targetEntity, RelationshipType relationshipType) {
        relations.put(targetEntity, relationshipType);
    }

    public Property getPropertyForRelClass(Class<?> targetEntity) throws SqlBuildException {
        if (targetEntity == null) {
            throw new MetadataException("targetEntity is null");
        }


        return getProperties().stream()
                .filter(item -> targetEntity.equals(item.targetEntity()))
                .findFirst()
                .orElseThrow(() -> new SqlBuildException("No relation property found"));
    }
}
