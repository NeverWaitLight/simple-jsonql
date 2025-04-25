package org.waitlight.simple.jsonql.engine;

import org.waitlight.simple.jsonql.jql.JsonQL;
import org.waitlight.simple.jsonql.metadata.MetadataSources;
import org.waitlight.simple.jsonql.metadata.PersistentClass;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UpdateEngine extends AbstractEngine {

    public UpdateEngine(MetadataSources metadataSources) {
        super(metadataSources);
    }

    /**
     * UPDATE table SET field1=value1, field2=value2 WHERE condition;
     */
    @Override
    public String parseSql(JsonQL jql) {
        PersistentClass entity = metadata.getEntityBinding(jql.update());
        List<String> setClauses = new ArrayList<>();

        // 处理 SET 子句
        for (Map.Entry<String, Object> entry : jql.set().entrySet()) {
            String value = entry.getValue() instanceof String ?
                    "'" + entry.getValue() + "'" :
                    String.valueOf(entry.getValue());
            setClauses.add(entry.getKey() + "=" + value);
        }

        // 处理 WHERE 子句
        StringBuilder whereClause = new StringBuilder();
        if (jql.where() != null && jql.where().condition() != null) {
            whereClause.append(" WHERE ").append(jql.where().condition());
        }

        return "UPDATE " +
                entity.getTableName() +
                " SET " +
                String.join(",", setClauses) +
                whereClause;
    }
} 