package org.waitlight.simple.jsonql;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PersistentClass {
    public String entityName;
    public String tableName;
    public List<Property> properties = new ArrayList<>();

    public void addProperty(Property property) {
        properties.add(property);
    }
}
