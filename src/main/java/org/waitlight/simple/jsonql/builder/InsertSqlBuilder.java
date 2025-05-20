package org.waitlight.simple.jsonql.builder;

import org.apache.calcite.jdbc.JavaTypeFactoryImpl;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.calcite.tools.RelBuilder;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.waitlight.simple.jsonql.metadata.*;
import org.waitlight.simple.jsonql.statement.InsertStatement;
import org.waitlight.simple.jsonql.statement.model.FieldStatement;
import org.waitlight.simple.jsonql.statement.model.NestedStatement;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class InsertSqlBuilder extends AbstractSqlBuilder<InsertStatement> {

    private static final Logger log = LoggerFactory.getLogger(InsertSqlBuilder.class);

    public static final String FOREIGN_KEY_PLACEHOLDER = "__FOREIGN_KEY_PLACEHOLDER__";

    public InsertSqlBuilder(Metadata metadata) {
        super(metadata);
    }

    /**
     * 根据实体元数据和语句内容，生成SQL语句
     * <p>
     * 处理流程：
     * 1. 获取实体元数据，检查是否有关系字段
     * 2. 如果没有关系字段，直接构建简单SQL返回
     * 3. 提取非嵌套的基本字段，构造主实体语句
     * 4. 按关系类型处理嵌套实体
     * 5. 生成并返回最终SQL，包含主实体和关联实体的SQL
     * <p>
     * 支持的关系类型：
     * - 单一实体创建
     * - 一对多关系（如User->Blog）
     * - 多对一关系（如Blog->User）
     *
     * @param stmt 待解析的CreateStatement语句
     * @return 包含SQL和参数的PreparedSql对象
     */
    @Override
    public PreparedSql<InsertStatement> build(InsertStatement stmt) throws SqlBuildeException {
        if (Objects.isNull(stmt)) {
            throw new SqlBuildeException("Statement is null");
        }

        final String mainEntityId = stmt.getEntityId();

        PersistentClass persistentClass = metadata.getEntity(mainEntityId);
        if (Objects.isNull(persistentClass)) {
            throw new MetadataException("Could not find metadata definition for entity: " + mainEntityId);
        }

        List<Property> relationProperties = persistentClass.getProperties().stream()
                .filter(prop -> Objects.nonNull(prop.relationship()))
                .toList();

        if (CollectionUtils.isEmpty(relationProperties)) {
            return buildSql(stmt);
        }

        List<FieldStatement> regularFields = stmt.getFields().stream()
                .filter(field -> CollectionUtils.isEmpty(field.getValues()))
                .toList();

        InsertStatement mainStmt = new InsertStatement();
        mainStmt.setEntityId(mainEntityId);
        mainStmt.setFields(new ArrayList<>(regularFields));

        final PreparedSql<InsertStatement> preparedSql = new PreparedSql<>();

        for (Property relationProperty : relationProperties) {
            RelationshipType relationType = relationProperty.relationship();

            switch (relationType) {
                case ONE_TO_MANY -> processOneToManyRelationship(stmt, mainStmt, relationProperty, preparedSql);
                case MANY_TO_ONE -> processManyToOneRelationship(stmt, mainStmt, relationProperty, preparedSql);
                default -> {
                }
            }
        }

        PreparedSql<InsertStatement> mainSql = buildSql(mainStmt);
        preparedSql.setSql(mainSql.getSql());
        preparedSql.setParameters(mainSql.getParameters());

        return preparedSql;
    }

    /**
     * 处理一对多关系（如User->Blog）
     * 为"多"方添加指向"一"方的外键字段占位符，并生成嵌套SQL
     *
     * @param stmt             原始语句
     * @param mainStmt         主实体语句
     * @param relationProperty 关系属性
     * @param preparedSql      预处理SQL对象
     */
    private void processOneToManyRelationship(InsertStatement stmt, InsertStatement mainStmt,
                                              Property relationProperty, PreparedSql<InsertStatement> preparedSql) throws SqlBuildeException {
        List<NestedStatement> nestedStatements = findNestedStatements(stmt, relationProperty.fieldName());

        if (CollectionUtils.isEmpty(nestedStatements)) {
            return;
        }

        for (NestedStatement nestedStmt : nestedStatements) {
            String foreignKeyFieldName = getForeignKeyName(mainStmt.getEntityId(),
                    nestedStmt.getEntityId(), relationProperty);

            if (StringUtils.isNotBlank(foreignKeyFieldName)) {
                FieldStatement foreignKeyField = new FieldStatement();
                foreignKeyField.setField(foreignKeyFieldName);
                foreignKeyField.setValue(FOREIGN_KEY_PLACEHOLDER);
                nestedStmt.getFields().add(foreignKeyField);
            }

            PreparedSql<InsertStatement> nestedSql = buildSql(nestedStmt);
            if (nestedSql.isNotEmpty()) {
                preparedSql.addNestedSQLs(nestedSql);
            }
        }
    }

    /**
     * 处理多对一关系（如Blog->User）
     * 检查嵌套语句是否只包含ID字段，如果是则将ID添加为外键
     *
     * @param stmt             原始语句
     * @param mainStmt         主实体语句
     * @param relationProperty 关系属性
     * @param preparedSql      预处理SQL对象
     */
    private void processManyToOneRelationship(InsertStatement stmt, InsertStatement mainStmt,
                                              Property relationProperty, PreparedSql<InsertStatement> preparedSql) {
        List<NestedStatement> nestedStatements = findNestedStatements(stmt, relationProperty.fieldName());

        if (CollectionUtils.isEmpty(nestedStatements)) {
            return;
        }

        NestedStatement nestedStmt = nestedStatements.get(0);

        if (isReferenceOnlyStatement(nestedStmt)) {
            String referencedId = extractIdFromNestedStatement(nestedStmt);

            if (StringUtils.isNotBlank(referencedId)) {
                addForeignKeyField(mainStmt, relationProperty, referencedId);
            }
        }
    }

    /**
     * 根据属性名查找嵌套语句
     *
     * @param stmt         原始语句
     * @param propertyName 属性名
     * @return 嵌套语句列表
     */
    private List<NestedStatement> findNestedStatements(InsertStatement stmt, String propertyName) {
        return stmt.getFields().stream()
                .filter(field -> StringUtils.equals(field.getField(), propertyName))
                .filter(field -> CollectionUtils.isNotEmpty(field.getValues()))
                .flatMap(field -> field.getValues().stream())
                .toList();
    }

    /**
     * 判断嵌套语句是否仅包含ID字段
     *
     * @param stmt 嵌套语句
     * @return 是否仅包含ID字段
     */
    private boolean isReferenceOnlyStatement(NestedStatement stmt) {
        if (StringUtils.isNotBlank(stmt.getDataId())) {
            return true;
        }

        if (CollectionUtils.isEmpty(stmt.getFields())) {
            return false;
        }

        if (stmt.getFields().size() == 1) {
            FieldStatement field = stmt.getFields().get(0);
            return "id".equals(field.getField());
        }

        return false;
    }

    /**
     * 从嵌套语句中提取ID值
     * 优先使用dataId，其次查找id字段
     *
     * @param stmt 嵌套语句
     * @return ID值
     */
    private String extractIdFromNestedStatement(NestedStatement stmt) {
        if (StringUtils.isNotBlank(stmt.getDataId())) {
            return stmt.getDataId();
        }

        if (CollectionUtils.isNotEmpty(stmt.getFields())) {
            for (FieldStatement field : stmt.getFields()) {
                if ("id".equals(field.getField()) && field.getValue() != null) {
                    return field.getValue().toString();
                }
            }
        }

        return null;
    }

    /**
     * 添加外键字段到主实体
     *
     * @param mainStmt         主实体语句
     * @param relationProperty 关系属性
     * @param foreignKeyValue  外键值
     */
    private void addForeignKeyField(InsertStatement mainStmt, Property relationProperty, String foreignKeyValue) {
        String foreignKeyFieldName = relationProperty.foreignKeyName();
        if (StringUtils.isBlank(foreignKeyFieldName)) {
            log.error("Could not determine foreign key field name for property {}", relationProperty.fieldName());
            return;
        }

        FieldStatement foreignKeyField = new FieldStatement();
        foreignKeyField.setField(foreignKeyFieldName);
        foreignKeyField.setValue(foreignKeyValue);
        mainStmt.getFields().add(foreignKeyField);
    }

    /**
     * 获取关联实体的外键字段名
     *
     * @param mainEntityId     主实体ID
     * @param relatedEntityId  关联实体ID
     * @param relationProperty 关系属性
     * @return 外键字段名
     */
    private String getForeignKeyName(String mainEntityId, String relatedEntityId, Property relationProperty) {
        if (StringUtils.isNotBlank(relationProperty.foreignKeyName())) {
            return relationProperty.foreignKeyName();
        }

        PersistentClass relatedEntityClass = metadata.getEntity(relatedEntityId);
        if (Objects.isNull(relatedEntityClass)) {
            return null;
        }

        for (Property property : relatedEntityClass.getProperties()) {
            if (property.relationship() != null) {
                Class<?> targetEntity = property.targetEntity();
                if (targetEntity != null && targetEntity.getSimpleName().equals(mainEntityId) &&
                        StringUtils.isNotBlank(property.foreignKeyName())) {
                    return property.foreignKeyName();
                }
            }
        }

        return mainEntityId.toLowerCase() + "_id";
    }

    /**
     * 构建SQL语句及其参数
     *
     * @param statement 实体语句
     * @return 预处理SQL对象
     */
    private PreparedSql<InsertStatement> buildSql(NestedStatement statement) throws SqlBuildeException {
        if (Objects.isNull(statement)) {
            return new PreparedSql<>();
        }

        List<String> fieldNames = statement.getFields().stream().map(FieldStatement::getField).toList();
        List<Object> parameters = statement.getFields().stream().map(FieldStatement::getValue).toList();

        if (fieldNames.isEmpty() || parameters.isEmpty()) {
            return new PreparedSql<>();
        }

        RelDataTypeFactory typeFactory = new JavaTypeFactoryImpl();
        RelDataTypeFactory.Builder relDataTypeBuilder = typeFactory.builder();
        Map<FieldStatement, Property> map = super.map(statement.getEntityId(), statement);
        for (FieldStatement field : statement.getFields()) {
            Property property = map.get(field);
            String columnName = property.columnName();
            SqlTypeName columnTypeName = property.columnTypeName();
            relDataTypeBuilder.add(columnName, columnTypeName);
        }
        RelDataType relDataType = relDataTypeBuilder.build();
        RelBuilder builder = RelBuilder.create(metadata.getFrameworkConfig());
        builder.values(relDataType, parameters.toArray());
        builder.push(builder.scan(statement.getEntityId()).build());
        RelNode relNode = builder.build();

        String sql = super.build(relNode);

        PreparedSql<InsertStatement> preparedSql = new PreparedSql<>(sql, parameters, InsertStatement.class);
        preparedSql.addRelNode(relNode);
        return preparedSql;
    }
}