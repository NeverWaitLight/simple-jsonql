package org.waitlight.simple.jsonql.metadata;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class PersistentClass {
    private Class<?> entityClass;
    private String entityName;

    private String tableName;
    private List<Property> properties = new ArrayList<>();

    public PersistentClass(Class<?> entityClazz, String entityName) {
        this.entityClass = entityClazz;
        this.entityName = entityName;
    }

    public void addProperty(Property property) {
        properties.add(property);
    }
}
