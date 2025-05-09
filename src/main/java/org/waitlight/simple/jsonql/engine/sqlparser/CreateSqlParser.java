package org.waitlight.simple.jsonql.engine.sqlparser;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.waitlight.simple.jsonql.metadata.*;
import org.waitlight.simple.jsonql.statement.CreateStatement;
import org.waitlight.simple.jsonql.statement.model.Field;
import org.waitlight.simple.jsonql.statement.model.NestedStatement;

import java.util.ArrayList;
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

    /**
     * 根据实体元数据和语句内容，生成SQL
     * 支持的关系类型：
     * - 单一实体创建
     * - 一对多关系（如User->Blog）
     * - 多对一关系（如Blog->User）
     */
    @Override
    public PreparedSql<CreateStatement> parseStmt2Sql(CreateStatement stmt) {
        if (Objects.isNull(stmt)) {
            return new PreparedSql<>();
        }

        final String mainEntityId = stmt.getEntityId();

        // 1. 先获取实体的元数据
        PersistentClass persistentClass = metadata.getEntity(mainEntityId);
        if (Objects.isNull(persistentClass)) {
            throw new MetadataException("Could not find metadata definition for entity: " + mainEntityId);
        }

        // 2. 检查实体是否有关系字段
        List<Property> relationProperties = persistentClass.getProperties().stream()
                .filter(prop -> Objects.nonNull(prop.getRelationshipType()))
                .toList();

        // 3. 如果实体没有任何关系字段，直接构建简单SQL
        if (CollectionUtils.isEmpty(relationProperties)) {
            return buildSql(stmt);
        }

        // 4. 提取基本字段（非嵌套字段）
        List<Field> regularFields = stmt.getFields().stream()
                .filter(field -> CollectionUtils.isEmpty(field.getValues()))
                .toList();

        // 5. 创建主实体语句
        CreateStatement mainStmt = new CreateStatement();
        mainStmt.setEntityId(mainEntityId);
        mainStmt.setFields(new ArrayList<>(regularFields));

        // 6. 初始化PreparedSql对象
        final PreparedSql<CreateStatement> preparedSql = new PreparedSql<>();

        // 7. 根据关系类型处理
        for (Property relationProperty : relationProperties) {
            RelationshipType relationType = relationProperty.getRelationshipType();

            switch (relationType) {
                case ONE_TO_MANY ->
                    processOneToManyRelationship(stmt, mainStmt, relationProperty, preparedSql);
                case MANY_TO_ONE ->
                    processManyToOneRelationship(stmt, mainStmt, relationProperty, preparedSql);
                default -> {
                }
            }
        }

        // 8. 生成主实体SQL
        PreparedSql<CreateStatement> mainSql = buildSql(mainStmt);
        preparedSql.setSql(mainSql.getSql());
        preparedSql.setParameters(mainSql.getParameters());

        return preparedSql;
    }

    /**
     * 处理一对多关系（如User->Blog）
     */
    private void processOneToManyRelationship(CreateStatement stmt, CreateStatement mainStmt,
            Property relationProperty, PreparedSql<CreateStatement> preparedSql) {
        // 查找对应的嵌套字段
        List<NestedStatement> nestedStatements = findNestedStatements(stmt, relationProperty.getName());

        // 如果没有找到嵌套数据，不做处理
        if (CollectionUtils.isEmpty(nestedStatements)) {
            return;
        }

        // 处理嵌套语句（一对多中的"多"）
        for (NestedStatement nestedStmt : nestedStatements) {
            // 获取"多"方实体中的外键字段名
            String foreignKeyFieldName = getForeignKeyFieldNameForRelatedEntity(mainStmt.getEntityId(),
                    nestedStmt.getEntityId(), relationProperty);

            if (StringUtils.isNotBlank(foreignKeyFieldName)) {
                // 为"多"方添加外键字段占位符
                Field foreignKeyField = new Field();
                foreignKeyField.setField(foreignKeyFieldName);
                foreignKeyField.setValue(FOREIGN_KEY_PLACEHOLDER);
                nestedStmt.getFields().add(foreignKeyField);
            }

            // 生成"多"方实体的SQL并添加到嵌套SQL中
            PreparedSql<CreateStatement> nestedSql = buildSql(nestedStmt);
            if (nestedSql.isNotEmpty()) {
                preparedSql.addNestedSQLs(nestedSql);
            }
        }
    }

    /**
     * 处理多对一关系（如Blog->User）
     */
    private void processManyToOneRelationship(CreateStatement stmt, CreateStatement mainStmt,
            Property relationProperty, PreparedSql<CreateStatement> preparedSql) {
        // 1. 查找对应的嵌套字段
        List<NestedStatement> nestedStatements = findNestedStatements(stmt, relationProperty.getName());

        // 2. 如果没有找到嵌套数据，不做处理
        if (CollectionUtils.isEmpty(nestedStatements)) {
            return;
        }

        // 3. 处理第一个嵌套语句（通常只有一个引用）
        NestedStatement nestedStmt = nestedStatements.get(0);

        // 4. 检查是否只包含ID字段（引用模式）- 特别处理如 Blog->User 的引用
        if (isReferenceOnlyStatement(nestedStmt)) {
            // 5. 从嵌套语句中提取ID值
            String referencedId = extractIdFromNestedStatement(nestedStmt);

            // 6. 如果有ID值，添加为外键
            if (StringUtils.isNotBlank(referencedId)) {
                // 直接添加到主语句的字段中
                addForeignKeyField(mainStmt, relationProperty, referencedId);
            }
        }
    }

    /**
     * 根据属性名查找嵌套语句
     */
    private List<NestedStatement> findNestedStatements(CreateStatement stmt, String propertyName) {
        return stmt.getFields().stream()
                .filter(field -> StringUtils.equals(field.getField(), propertyName))
                .filter(field -> CollectionUtils.isNotEmpty(field.getValues()))
                .flatMap(field -> field.getValues().stream())
                .toList();
    }

    /**
     * 判断嵌套语句是否仅包含ID字段
     */
    private boolean isReferenceOnlyStatement(NestedStatement stmt) {
        if (StringUtils.isNotBlank(stmt.getDataId())) {
            return true;
        }

        if (CollectionUtils.isEmpty(stmt.getFields())) {
            return false;
        }

        // 检查是否只有一个"id"字段
        if (stmt.getFields().size() == 1) {
            Field field = stmt.getFields().get(0);
            return "id".equals(field.getField());
        }

        return false;
    }

    /**
     * 从嵌套语句中提取ID值
     */
    private String extractIdFromNestedStatement(NestedStatement stmt) {
        // 1. 优先使用dataId
        if (StringUtils.isNotBlank(stmt.getDataId())) {
            return stmt.getDataId();
        }

        // 2. 查找id字段
        if (CollectionUtils.isNotEmpty(stmt.getFields())) {
            for (Field field : stmt.getFields()) {
                if ("id".equals(field.getField()) && field.getValue() != null) {
                    return field.getValue().toString();
                }
            }
        }

        return null;
    }

    /**
     * 添加外键字段到主实体
     */
    private void addForeignKeyField(CreateStatement mainStmt, Property relationProperty, String foreignKeyValue) {
        String foreignKeyFieldName = relationProperty.getForeignKeyName();
        if (StringUtils.isBlank(foreignKeyFieldName)) {
            log.error("Could not determine foreign key field name for property {}", relationProperty.getName());
            return;
        }

        Field foreignKeyField = new Field();
        foreignKeyField.setField(foreignKeyFieldName);
        foreignKeyField.setValue(foreignKeyValue);
        mainStmt.getFields().add(foreignKeyField);
    }

    /**
     * 获取关联实体的外键字段名
     */
    private String getForeignKeyFieldNameForRelatedEntity(String mainEntityId, String relatedEntityId,
            Property relationProperty) {
        // 如果关联属性已经指定了外键名称，直接使用
        if (StringUtils.isNotBlank(relationProperty.getForeignKeyName())) {
            return relationProperty.getForeignKeyName();
        }

        // 尝试从目标实体的属性中找到外键
        PersistentClass relatedEntityClass = metadata.getEntity(relatedEntityId);
        if (Objects.isNull(relatedEntityClass)) {
            log.error("Could not find metadata for related entity: {}", relatedEntityId);
            return null;
        }

        for (Property property : relatedEntityClass.getProperties()) {
            if (property.getRelationshipType() != null) {
                Class<?> targetEntity = property.getTargetEntity();
                if (targetEntity != null && targetEntity.getSimpleName().equals(mainEntityId) &&
                        StringUtils.isNotBlank(property.getForeignKeyName())) {
                    return property.getForeignKeyName();
                }
            }
        }

        // 使用默认命名规则
        return mainEntityId.toLowerCase() + "_id";
    }

    /**
     * 构建SQL语句
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
}