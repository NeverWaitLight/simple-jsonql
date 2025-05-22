package org.waitlight.simple.jsonql.builder;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.SQLDialect;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.waitlight.simple.jsonql.metadata.Metadata;
import org.waitlight.simple.jsonql.metadata.PersistentClass;
import org.waitlight.simple.jsonql.metadata.Property;
import org.waitlight.simple.jsonql.metadata.RelationshipType;
import org.waitlight.simple.jsonql.statement.InsertStatement;
import org.waitlight.simple.jsonql.statement.StatementUtils;
import org.waitlight.simple.jsonql.statement.StatementsPairs;
import org.waitlight.simple.jsonql.statement.model.FieldStatement;
import org.waitlight.simple.jsonql.statement.model.PersistStatement;

import java.util.*;

public class InsertSqlBuilder extends AbstractPersistSqlBuilder<InsertStatement> {

    private static final Logger log = LoggerFactory.getLogger(InsertSqlBuilder.class);

    public InsertSqlBuilder(Metadata metadata) {
        super(metadata);
    }

    @Override
    public PreparedSql<InsertStatement> build(InsertStatement statement) throws SqlBuildException {
        if (Objects.isNull(statement)) {
            throw new SqlBuildException("Statement is null");
        }
        final String mainEntityId = statement.getEntityId();

        final PersistentClass mainPersistentClass = metadata.getEntity(mainEntityId);
        final StatementsPairs<PersistStatement> statementsPairs = StatementUtils.convert2SingleLevel(statement);

        final PersistStatement mainStatement = statementsPairs.mainStatement();
        final PreparedSql<InsertStatement> preparedSql = buildSql(mainStatement);

        for (PersistStatement nestedStatement : statementsPairs.nestedStatements()) {
            String nestedEntityId = nestedStatement.getEntityId();
            PersistentClass nestedPersistentClass = metadata.getEntity(nestedEntityId);
            RelationshipType relationshipType = mainPersistentClass.getRelations().get(nestedPersistentClass.getEntityClass());
            switch (relationshipType) {
                case ONE_TO_MANY ->
                        processOneToMany(mainPersistentClass, nestedPersistentClass, mainStatement, nestedStatement, preparedSql);
                case MANY_TO_ONE ->
                        processManyToOne(mainPersistentClass, nestedPersistentClass, mainStatement, nestedStatement, preparedSql);
                default -> {

                }
            }
        }

        return preparedSql;
    }

    private void processOneToMany(
            PersistentClass mainPersistentClass,
            PersistentClass nestedPersistentClass,
            PersistStatement mainStatement,
            PersistStatement nestedStatement,
            PreparedSql<InsertStatement> preparedSql
    ) throws SqlBuildException {

        String fieldName = Optional.ofNullable(nestedPersistentClass.getPropertyForRelClass(mainPersistentClass.getEntityClass()))
                .map(Property::fieldName)
                .orElseThrow(() -> new SqlBuildException("No relation property found"));

        FieldStatement relField = new FieldStatement();
        relField.setField(fieldName);
        relField.setValue(FOREIGN_KEY_PLACEHOLDER);
        nestedStatement.getFields().add(relField);

        preparedSql.addNestedSQLs(buildSql(nestedStatement));
    }

    private void processManyToOne(
            PersistentClass mainPersistentClass,
            PersistentClass nestedPersistentClass,
            PersistStatement mainStatement,
            PersistStatement nestedStatement,
            PreparedSql<InsertStatement> preparedSql) {

        FieldStatement relField = new FieldStatement();
//        relField.setField(relationProperty.columnName());
        relField.setValue(FOREIGN_KEY_PLACEHOLDER);
//        statement.getFields().add(relField);
//
//        preparedSql.addNestedSQLs(buildSql(statement));
//
//        if (CollectionUtils.isEmpty(persistStatements)) {
//            return;
//        }
//
//        PersistStatement nestedStmt = persistStatements.get(0);
//
//        if (isReferenceOnlyStatement(nestedStmt)) {
//            String referencedId = extractIdFromNestedStatement(nestedStmt);
//
//            if (StringUtils.isNotBlank(referencedId)) {
//                addForeignKeyField(mainStmt, relationProperty, referencedId);
//            }
//        }
    }


    /**
     * 判断嵌套语句是否仅包含ID字段
     *
     * @param stmt 嵌套语句
     * @return 是否仅包含ID字段
     */
    private boolean isReferenceOnlyStatement(PersistStatement stmt) {
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
    private String extractIdFromNestedStatement(PersistStatement stmt) {
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
    private PreparedSql<InsertStatement> buildSql(PersistStatement statement) throws SqlBuildException {
        if (Objects.isNull(statement)) {
            return new PreparedSql<>();
        }

        List<Object> parameters = statement.getFields().stream().map(FieldStatement::getValue).toList();

        DSLContext create = DSL.using(SQLDialect.MYSQL);
        Table<?> table = DSL.table(DSL.name(statement.getEntityId()));

        List<Field<Object>> fields = new ArrayList<>();
        List<Object> values = new ArrayList<>();

        Map<FieldStatement, Property> map = map(statement.getEntityId(), statement);
        for (FieldStatement field : statement.getFields()) {
            Property property = map.get(field);
            String columnName = property.columnName();
            fields.add(DSL.field(DSL.name(columnName)));
            values.add(field.getValue());
        }

        String sql = create.insertInto(table)
                .columns(fields.toArray(new Field[0]))
                .values(values.toArray())
                .getSQL();
        log.info("build sql: {}", sql);
        return new PreparedSql<>(sql, parameters, InsertStatement.class);
    }
}