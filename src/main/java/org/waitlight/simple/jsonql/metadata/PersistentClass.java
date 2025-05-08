package org.waitlight.simple.jsonql.metadata;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PersistentClass {
    private Class<?> entityClass;
    private String entityName;
    private String tableName;
    private List<Property> properties = new ArrayList<>();

    public void addProperty(Property property) {
        properties.add(property);
    }
}
