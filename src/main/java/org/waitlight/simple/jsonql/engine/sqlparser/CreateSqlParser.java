package org.waitlight.simple.jsonql.engine.sqlparser;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.waitlight.simple.jsonql.metadata.Metadata;
import org.waitlight.simple.jsonql.metadata.MetadataException;
import org.waitlight.simple.jsonql.metadata.PersistentClass;
import org.waitlight.simple.jsonql.metadata.Property;
import org.waitlight.simple.jsonql.statement.CreateStatement;
import org.waitlight.simple.jsonql.statement.model.Field;
import org.waitlight.simple.jsonql.statement.model.NestedStatement;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class CreateSqlParser implements SqlParser<CreateStatement> {

    private static final Logger log = LoggerFactory.getLogger(CreateSqlParser.class);
    private final Metadata metadata;

    public static final String FOREIGN_KEY_PLACEHOLDER = "__FOREIGN_KEY_PLACEHOLDER__";

    public CreateSqlParser(Metadata metadata) {
        this.metadata = metadata;
    }

    // Extract the associated attributes
    public List<Property> extractRelationshipProperties(String entityId) {
        PersistentClass persistentClass = metadata.getEntity(entityId);
        if (Objects.isNull(persistentClass)) {
            throw new MetadataException("Could not find metadata definition for entity: " + entityId);
        }

        return persistentClass.getProperties().stream()
                .filter(prop -> Objects.nonNull(prop.getRelationshipType()))
                .toList();
    }

    public PreparedSql<CreateStatement> parseStmtToSql(CreateStatement stmt) {
        /**
         * 1. 判断是 oneToOne \ oneToMany \ manyToMany 的哪一种
         * 2.1 oneToOne 
         * 2.2 生成 主动 方 insert sql, 生成 preparedsql
         * 2.3 生成 被动 方 insert sql, 放入 preparedsql 中的 nestedsql 字段
         * 3.1 oneToMany 
         * 3.2 生成 one 方 insert sql, 生成 preparedsql
         * 3.3 生成 many 方 insert sql, 放入 preparedsql 中的nestedsql字段
         * 4.1 manyToMany 
         * 4.2 生成 many 方 insert sql, 处理好many方的表中有one方的id字段生成 preparedsql
         */

        return null;
    }

    @Override
    public PreparedSql<CreateStatement> parseStmt2Sql(CreateStatement stmt) {
        final String mainEntityId = stmt.getEntityId();

        List<Property> relationshipProperties = extractRelationshipProperties(mainEntityId);
        // 有关系字段
        if (CollectionUtils.isNotEmpty(relationshipProperties)) {
            // if 是 one 方
            // 1.生成 one 方 insert sql, 生成 preparedsql
            // 2.生成 many 方 insert sql, 放入 preparedsql 中的nestedsql字段

            // else 是 many 方
            // 1.生成 many 方 insert sql, 处理好many方的表中有one方的id字段生成 preparedsql
        } else {
            // 没有关系字段
            // 按照处理 one 方的逻辑生成preparedsql
        }

        // 1. 处理主实体
        List<Field> mainFields = stmt.getFields().stream()
                .filter(field -> CollectionUtils.isEmpty(field.getValues()))
                .toList();
        List<Field> nestedFields = stmt.getFields().stream()
                .filter(field -> CollectionUtils.isNotEmpty(field.getValues()))
                .toList();

        // 2. 生成主实体的SQL，创建一个新的CreateStatement避免修改原始对象
        CreateStatement mainStmt = new CreateStatement();
        mainStmt.setEntityId(mainEntityId);
        mainStmt.setFields(mainFields);

        PreparedSql<CreateStatement> preparedSql = buildSql(mainStmt);

        // 3. 处理嵌套实体，使用FOREIGN_KEY_PLACEHOLDER常量标记需要替换的外键
        for (Field nestedField : nestedFields) {
            for (NestedStatement nestedStatement : nestedField.getValues()) {

                final String nestedEntityId = nestedStatement.getEntityId();
                if (StringUtils.isBlank(nestedEntityId)) {
                    log.error("Could not determine entityId for a nested object within field '{}'. Skipping.",
                            nestedField.getField());
                    continue;
                }

                String foreignKeyFieldName = getForeignKeyFieldName(mainEntityId, nestedEntityId);
                if (StringUtils.isBlank(foreignKeyFieldName)) {
                    log.error(
                            "Could not determine foreign key field name for relation {} -> {}. Skipping nested inserts for this object.",
                            stmt.getEntityId(), nestedEntityId);
                    continue;
                }

                // 添加外键字段占位符
                Field foreignKeyField = new Field();
                foreignKeyField.setField(foreignKeyFieldName);
                foreignKeyField.setValue(FOREIGN_KEY_PLACEHOLDER);
                nestedStatement.getFields().add(foreignKeyField);

                PreparedSql<CreateStatement> nestedSql = buildSql(nestedStatement);
                if (nestedSql.isNotEmpty()) {
                    preparedSql.addNestedSQLs(nestedSql);
                }
            }
        }

        return preparedSql;
    }

    private PreparedSql<CreateStatement> buildSql(NestedStatement entity) {
        if (Objects.isNull(entity)) {
            return new PreparedSql<>();
        }

        List<String> fieldNames = entity.getFields().stream().map(Field::getField).toList();
        List<Object> parameters = entity.getFields().stream().map(Field::getValue).toList();

        if (fieldNames.isEmpty() || parameters.isEmpty()) {
            return new PreparedSql<>();
        }

        String sql = "INSERT INTO " +
                entity.getEntityId() +
                " (" +
                String.join(", ", fieldNames) +
                ") VALUES (" +
                String.join(", ", Collections.nCopies(fieldNames.size(), "?")) +
                ")";

        return new PreparedSql<>(sql, parameters, CreateStatement.class);
    }

    private String getForeignKeyFieldName(String mainEntityName, String nestedEntityName) {
        PersistentClass mainEntityClass = metadata.getEntity(mainEntityName);
        PersistentClass nestedEntityClass = metadata.getEntity(nestedEntityName);

        if (ObjectUtils.anyNull(mainEntityClass, nestedEntityClass)) {
            log.warn("Entity not found in metadata: parent={}, child={}", mainEntityName, nestedEntityName);
            return null;
        }

        for (Property property : nestedEntityClass.getProperties()) {
            String foreignKeyName = property.getForeignKeyName();
            if (StringUtils.isNotBlank(foreignKeyName)) {
                return foreignKeyName;
            }
        }

        throw new MetadataException(String.format("Could not determine foreign key field name for relation %s -> %s", mainEntityName, nestedEntityName));
    }
}