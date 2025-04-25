package org.waitlight.simple.jsonql.engine;

import org.waitlight.simple.jsonql.jql.JsonQL;
import org.waitlight.simple.jsonql.metadata.MetadataSources;
import org.waitlight.simple.jsonql.metadata.PersistentClass;
import org.waitlight.simple.jsonql.metadata.Property;

import java.util.ArrayList;
import java.util.List;

public class SelectEngine extends AbstractEngine {

    public SelectEngine(MetadataSources metadataSources) {
        super(metadataSources);
    }

    @Override
    public String parseSql(JsonQL jql) {
        List<String> selectFields = new ArrayList<>(jql.select());
        PersistentClass entity = metadata.getEntityBinding(jql.from());

        StringBuilder sql = new StringBuilder();
        StringBuilder join = new StringBuilder();

        // 替换关联字段为实际的表字段
        for (Property prop : entity.getProperties()) {
            if (selectFields.contains(prop.getName()) && prop.getRelationshipType() != null) {

                selectFields.remove(prop.getName());
                PersistentClass targetEntity = metadata.getEntityBinding(prop.getTargetEntity().getSimpleName().toLowerCase());

                // 添加目标表字段
                for (Property targetProp : targetEntity.getProperties()) {
                    if (!"relationshipType".equals(targetProp.getName())) {
                        selectFields.add(String.format("%s.%s as %s_%s",
                                targetEntity.getTableName().toLowerCase(),
                                targetProp.getColumn(),
                                prop.getName(),
                                targetProp.getName()));
                    }
                }

                join.append(" LEFT JOIN ")
                        .append(targetEntity.getTableName().toLowerCase())
                        .append(" ON ")
                        .append(targetEntity.getTableName().toLowerCase())
                        .append(".")
                        .append(prop.getForeignKeyName())
                        .append(" = ")
                        .append(entity.getTableName().toLowerCase())
                        .append(".id");
            }
        }
        sql.append("SELECT ")
                .append(String.join(", ", selectFields))
                .append(" FROM ")
                .append(entity.getTableName().toLowerCase());

        // 处理WHERE
        if (jql.where() != null) {
            sql.append(" WHERE ")
                    .append(jql.where().condition());
        }
        sql.append(join);
        return sql.toString();
    }
}
