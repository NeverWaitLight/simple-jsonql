package org.waitlight.simple.jsonql.engine.sqlparser;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
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
         * 输入无JoinCloumn注解一方的statement，statement中包含有JoinCloumn注解一方的新增对象时
         * 生成无JoinCloumn注解一方的insert sql, 生成 preparedsql
         * 生成有JoinCloumn注解一方的insert sql, 放入 preparedsql 中的nestedsql字段
         *
         * 输入有JoinCloumn注解一方的statement，statement中包含有无JoinCloumn注解一方的已有数据的id时
         * 生成有JoinCloumn注解一方的insert sql, 生成 preparedsql 包含无JoinCloumn注解一方的id字段
         * 3.1 oneToMany
         * 输入无JoinCloumn注解一方的statement，statement中包含有JoinCloumn注解一方的新增对象时
         * 生成无JoinCloumn注解一方的insert sql, 生成 preparedsql
         * 生成有JoinCloumn注解一方的insert sql, 放入 preparedsql 中的nestedsql字段
         *
         * 输入有JoinCloumn注解一方的statement，statement中包含有无JoinCloumn注解一方的已有数据的id时
         * 生成有JoinCloumn注解一方的insert sql, 生成 preparedsql 包含无JoinCloumn注解一方的id字段
         * 4.1 manyToMany
         * 输入无JoinCloumn注解一方的statement，statement中包含有JoinCloumn注解一方的已有数据的id时
         * 生成无JoinCloumn注解一方的insert sql, 生成 preparedsql
         * 生成有JoinCloumn注解一方的insert sql, 放入 preparedsql 中的nestedsql字段
         *
         * 输入有JoinCloumn注解一方的statement，statement中包含有无JoinCloumn注解一方的已有数据的id时
         * 生成有JoinCloumn注解一方的insert sql, 生成 preparedsql 包含无JoinCloumn注解一方的id字段
         * 
         */

        if (Objects.isNull(stmt)) {
            return new PreparedSql<>();
        }

        final String mainEntityId = stmt.getEntityId();

        // 1. 先获取实体的关系属性
        List<Property> relationProperties = extractRelationshipProperties(mainEntityId);

        // 2. 如果没有关系属性，直接生成简单的insert SQL
        if (CollectionUtils.isEmpty(relationProperties)) {
            return buildSql(stmt);
        }

        // 3. 处理带有关系的实体
        // 3.1 先处理主实体的基本字段
        List<Field> regularFields = stmt.getFields().stream()
                .filter(field -> CollectionUtils.isEmpty(field.getValues()))
                .toList();
        // 3.2 创建主实体的语句
        CreateStatement mainStmt = new CreateStatement();
        mainStmt.setEntityId(mainEntityId);
        mainStmt.setFields(regularFields);

        // 4. 处理关系字段
        List<Field> nestedFields = stmt.getFields().stream()
                .filter(field -> CollectionUtils.isNotEmpty(field.getValues()))
                .toList();
        // 如果没有嵌套实体，可以直接生成主实体的SQL
        if (CollectionUtils.isEmpty(nestedFields)) {
            return buildSql(mainStmt);
        }

        // 5. 处理嵌套实体
        final PreparedSql<CreateStatement> preparedSql = new PreparedSql<>();
        for (Field nestedField : nestedFields) {
            // 找到对应的关系属性
            Property relationProperty = relationProperties.stream()
                    .filter(prop -> StringUtils.equals(prop.getName(), nestedField.getField()))
                    .findFirst()
                    .orElse(null);
            if (Objects.isNull(relationProperty)) {
                continue;
            }

            RelationshipType relationType = relationProperty.getRelationshipType();
            boolean isOwner = StringUtils.isBlank(relationProperty.getMappedBy());

            for (NestedStatement nestedStmt : nestedField.getValues()) {
                // 根据关系类型和是否是拥有方，决定处理逻辑
                switch (relationType) {
                    case ONE_TO_ONE ->
                            processOneToOneRelation(mainStmt, nestedStmt, relationProperty, isOwner, preparedSql);
                    case ONE_TO_MANY -> processOneToManyRelation(mainStmt, nestedStmt, relationProperty, preparedSql);
                    case MANY_TO_ONE -> processManyToOneRelation(mainStmt, nestedStmt, relationProperty, preparedSql);
                    case MANY_TO_MANY ->
                            processManyToManyRelation(mainStmt, nestedStmt, relationProperty, isOwner, preparedSql);
                    default -> log.warn("Unsupported relationship type: {}", relationType);
                }
            }
        }

        // 6. 生成主实体SQL
        PreparedSql<CreateStatement> mainSql = buildSql(mainStmt);
        preparedSql.setSql(mainSql.getSql());
        preparedSql.setParameters(mainSql.getParameters());

        return preparedSql;
    }

    /**
     * 获取关联实体的名称
     */
    private String getRelatedEntityName(Property relationProperty) {
        if (relationProperty.getTargetEntity() != null) {
            return relationProperty.getTargetEntity().getSimpleName();
        }
        return null;
    }

    /**
     * 处理一对一关系
     */
    private void processOneToOneRelation(CreateStatement mainStmt,
                                         NestedStatement nestedStmt,
                                         Property relationProperty,
                                         boolean isOwner,
                                         PreparedSql<CreateStatement> preparedSql) {
        if (isOwner) {
            // 当前实体是关系拥有方（有JoinColumn注解的一方）
            // 把嵌套实体的ID作为外键添加到当前实体
            if (StringUtils.isNotBlank(nestedStmt.getDataId())) {
                addForeignKeyField(mainStmt, relationProperty, nestedStmt.getDataId());
            }
        } else {
            // 当前实体是关系被拥有方（无JoinColumn注解的一方）
            // 先生成当前实体SQL，然后为嵌套实体添加指向当前实体的外键

            // 1. 嵌套实体中添加指向主实体的外键字段占位符
            String foreignKeyFieldName = getForeignKeyFieldNameForRelatedEntity(mainStmt.getEntityId(),
                    nestedStmt.getEntityId(), relationProperty);
            if (StringUtils.isNotBlank(foreignKeyFieldName)) {
                Field foreignKeyField = new Field();
                foreignKeyField.setField(foreignKeyFieldName);
                foreignKeyField.setValue(FOREIGN_KEY_PLACEHOLDER);
                nestedStmt.getFields().add(foreignKeyField);
            }

            // 2. 生成嵌套实体SQL并添加到preparedSql
            PreparedSql<CreateStatement> nestedSql = buildSql(nestedStmt);
            if (nestedSql.isNotEmpty()) {
                preparedSql.addNestedSQLs(nestedSql);
            }
        }
    }

    /**
     * 处理一对多关系
     */
    private void processOneToManyRelation(CreateStatement mainStmt, NestedStatement nestedStmt,
                                          Property relationProperty, PreparedSql<CreateStatement> preparedSql) {
        boolean isOneToManySide = relationProperty.getRelationshipType() == RelationshipType.ONE_TO_MANY;

        if (isOneToManySide) {
            // 当前实体是"一"的一方
            // 生成主实体SQL，然后为"多"的一方添加外键

            // 1. 嵌套实体中添加指向主实体的外键字段占位符
            String foreignKeyFieldName = getForeignKeyFieldNameForRelatedEntity(mainStmt.getEntityId(),
                    nestedStmt.getEntityId(), relationProperty);
            if (StringUtils.isNotBlank(foreignKeyFieldName)) {
                Field foreignKeyField = new Field();
                foreignKeyField.setField(foreignKeyFieldName);
                foreignKeyField.setValue(FOREIGN_KEY_PLACEHOLDER);
                nestedStmt.getFields().add(foreignKeyField);
            }

            // 2. 生成嵌套实体SQL并添加到preparedSql
            PreparedSql<CreateStatement> nestedSql = buildSql(nestedStmt);
            if (nestedSql.isNotEmpty()) {
                preparedSql.addNestedSQLs(nestedSql);
            }
        } else {
            // 当前实体是"多"的一方
            // 把"一"的一方的ID作为外键添加到当前实体
            if (StringUtils.isNotBlank(nestedStmt.getDataId())) {
                addForeignKeyField(mainStmt, relationProperty, nestedStmt.getDataId());
            }
        }
    }

    /**
     * 处理多对一关系
     */
    private void processManyToOneRelation(CreateStatement mainStmt, NestedStatement nestedStmt,
                                          Property relationProperty, PreparedSql<CreateStatement> preparedSql) {
        // 当前实体是"多"的一方
        // 添加"一"的一方的ID作为外键
        if (StringUtils.isNotBlank(nestedStmt.getDataId())) {
            addForeignKeyField(mainStmt, relationProperty, nestedStmt.getDataId());
        }
    }

    /**
     * 处理多对多关系
     */
    private void processManyToManyRelation(CreateStatement mainStmt, NestedStatement nestedStmt,
                                           Property relationProperty, boolean isOwner,
                                           PreparedSql<CreateStatement> preparedSql) {
        // 多对多关系需要使用中间表
        String joinTableName = relationProperty.getJoinTableName();
        if (StringUtils.isBlank(joinTableName)) {
            log.error("Missing join table name for many-to-many relationship between {} and {}",
                    mainStmt.getEntityId(), nestedStmt.getEntityId());
            return;
        }

        // 创建关联表的插入语句
        List<Field> joinTableFields = new ArrayList<>();

        // 添加主实体外键字段
        String mainEntityFkName = "";
        if (relationProperty.getJoinColumns() != null && relationProperty.getJoinColumns().length > 0) {
            mainEntityFkName = relationProperty.getJoinColumns()[0].name();
        }

        if (StringUtils.isBlank(mainEntityFkName)) {
            mainEntityFkName = mainStmt.getEntityId().toLowerCase() + "_id";
        }

        Field mainEntityFkField = new Field();
        mainEntityFkField.setField(mainEntityFkName);
        mainEntityFkField.setValue(FOREIGN_KEY_PLACEHOLDER);
        joinTableFields.add(mainEntityFkField);

        // 添加关联实体外键字段
        String targetEntityFkName = "";
        if (relationProperty.getInverseJoinColumns() != null && relationProperty.getInverseJoinColumns().length > 0) {
            targetEntityFkName = relationProperty.getInverseJoinColumns()[0].name();
        }

        if (StringUtils.isBlank(targetEntityFkName)) {
            targetEntityFkName = nestedStmt.getEntityId().toLowerCase() + "_id";
        }

        Field targetEntityFkField = new Field();
        targetEntityFkField.setField(targetEntityFkName);
        targetEntityFkField.setValue(nestedStmt.getDataId());
        joinTableFields.add(targetEntityFkField);

        // 创建中间表的嵌套语句
        NestedStatement joinTableStmt = new NestedStatement();
        joinTableStmt.setEntityId(joinTableName);
        joinTableStmt.setFields(joinTableFields);

        // 生成中间表SQL并添加到主SQL
        PreparedSql<CreateStatement> joinTableSql = buildSql(joinTableStmt);
        if (joinTableSql.isNotEmpty()) {
            preparedSql.addNestedSQLs(joinTableSql);
        }
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

    @Override
    public PreparedSql<CreateStatement> parseStmt2Sql(CreateStatement stmt) {
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

        throw new MetadataException(String.format("Could not determine foreign key field name for relation %s -> %s",
                mainEntityName, nestedEntityName));
    }
}