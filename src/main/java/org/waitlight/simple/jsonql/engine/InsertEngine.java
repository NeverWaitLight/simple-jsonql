package org.waitlight.simple.jsonql.engine;

import org.waitlight.simple.jsonql.jql.JsonQL;
import org.waitlight.simple.jsonql.metadata.MetadataSources;
import org.waitlight.simple.jsonql.metadata.PersistentClass;

import java.util.ArrayList;
import java.util.List;

public class InsertEngine extends AbstractEngine {

    public InsertEngine(MetadataSources metadataSources) {
        super(metadataSources);
    }

    /**
     * INSERT INTO table (field1, field2) values(value1, value2);
     */
    @Override
    public String parseSql(JsonQL jql) {
        List<String> fields = new ArrayList<>(jql.values().keySet());
        PersistentClass entity = metadata.getEntityBinding(jql.into());

        return "INSERT INTO " +
                entity.getTableName() +
                "(" +
                String.join(",", fields) +
                ") VALUES (" +
                String.join(",", jql.values().values().stream().map(str -> "'" + str + "'").toList()) +
                ")";
    }
}
