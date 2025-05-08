package org.waitlight.simple.jsonql.engine.sqlparser;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.waitlight.simple.jsonql.metadata.Metadata;
import org.waitlight.simple.jsonql.metadata.PersistentClass;
import org.waitlight.simple.jsonql.metadata.Property;
import org.waitlight.simple.jsonql.metadata.RelationshipType;
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

    @Override
    public PreparedSql<CreateStatement> parseSql(CreateStatement stmt) {
        final String mainEntityId = stmt.getEntityId();

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

    /**
     * 构建INSERT SQL语句
     *
     * @param entity 实体对象（CreateStatement或NestedEntity）
     * @return PreparedSql对象
     */
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

    /**
     * 获取外键字段名称
     *
     * @param mainEntityName   父实体名称
     * @param nestedEntityName 子实体名称
     * @return 外键字段名称，如果无法确定则返回null
     */
    private String getForeignKeyFieldName(String mainEntityName, String nestedEntityName) {
        PersistentClass mainEntityClass = metadata.getEntityBinding(mainEntityName);
        PersistentClass nestedEntityClass = metadata.getEntityBinding(nestedEntityName);

        if (ObjectUtils.anyNull(mainEntityClass, nestedEntityClass)) {
            log.warn("Entity not found in metadata: parent={}, child={}", mainEntityName, nestedEntityName);
            throw new UnsupportedOperationException(
                    "Entity not found in metadata: parent=" + mainEntityName + ", child=" + nestedEntityName);
        }

        // 查找从 nestedEntityClass 指向 mainEntityClass 的外键
        for (Property property : nestedEntityClass.getProperties()) {
            if (property.getRelationshipType() != null &&
                    property.getRelationshipType() == RelationshipType.MANY_TO_ONE &&
                    mainEntityClass.getEntityName().equals(property.getTargetEntity().getSimpleName())) {
                return property.getColumn();
            }
        }

        // 查找从 mainEntityClass 指向 nestedEntityClass 的外键 (通常用于一对多，子表持有外键)
        // 但在Create场景，通常是子实体持有指向父实体的外键
        // 如果是双向一对一，也可能是主表持有外键，但插入时通常先插入没有外键依赖的一方，或允许外键为空
        // 此处逻辑假设子实体表（nestedEntityName）包含指向父实体表（mainEntityName）的外键
        log.warn("Could not determine foreign key from {} to {} by checking MANY_TO_ONE in {}. " +
                "Also checking ONE_TO_MANY in {} (less common for create foreign key)",
                nestedEntityName, mainEntityName, nestedEntityName, mainEntityName);

        for (Property property : mainEntityClass.getProperties()) {
            if (property.getRelationshipType() != null &&
                    property.getRelationshipType() == RelationshipType.ONE_TO_MANY &&
                    nestedEntityClass.getEntityName().equals(property.getTargetEntity().getSimpleName())) {
                // 在ONE_TO_MANY关系中，外键在"多"的一方（即nestedEntityClass）
                // 需要找到nestedEntityClass中对应的mappedBy字段
                PersistentClass targetPc = metadata.getEntityBinding(property.getTargetEntity().getSimpleName());
                if (targetPc != null) {
                    for (Property targetProp : targetPc.getProperties()) {
                        if (property.getMappedBy().equals(targetProp.getName()) &&
                                targetProp.getRelationshipType() != null &&
                                targetProp.getRelationshipType() == RelationshipType.MANY_TO_ONE &&
                                mainEntityClass.getEntityName().equals(targetProp.getTargetEntity().getSimpleName())) {
                            return targetProp.getColumn();
                        }
                    }
                }
            }
        }
        log.error("Foreign key relationship not found between {} and {}", mainEntityName, nestedEntityName);
        return null; // 或者抛出更具体的异常
    }
}