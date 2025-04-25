package org.waitlight.simple.jsonql.engine;

import org.waitlight.simple.jsonql.jql.JsonQL;
import org.waitlight.simple.jsonql.metadata.MetadataSources;
import org.waitlight.simple.jsonql.metadata.PersistentClass;

public class DeleteEngine extends AbstractEngine {

    public DeleteEngine(MetadataSources metadataSources) {
        super(metadataSources);
    }

    /**
     * DELETE FROM table WHERE condition;
     */
    @Override
    public String parseSql(JsonQL jql) {
        PersistentClass entity = metadata.getEntityBinding(jql.from());
        StringBuilder whereClause = new StringBuilder();

        // 处理 WHERE 子句
        if (jql.where() != null && jql.where().condition() != null) {
            whereClause.append(" WHERE ").append(jql.where().condition());
        }

        return "DELETE FROM " +
                entity.getTableName() +
                whereClause;
    }
} 