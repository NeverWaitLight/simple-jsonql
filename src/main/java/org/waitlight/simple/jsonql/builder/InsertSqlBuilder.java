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
        PreparedSql<InsertStatement> preparedSql = buildSql(mainStatement);

        for (PersistStatement nestedStatement : statementsPairs.nestedStatements()) {
            String nestedEntityId = nestedStatement.getEntityId();
            PersistentClass nestedPersistentClass = metadata.getEntity(nestedEntityId);
            RelationshipType relationshipType = mainPersistentClass.getRelations()
                    .get(nestedPersistentClass.getEntityClass());
            switch (relationshipType) {
                case ONE_TO_MANY ->
                        processOneToMany(mainPersistentClass, nestedPersistentClass, mainStatement, nestedStatement,
                                preparedSql);
                case MANY_TO_ONE ->
                    preparedSql = processManyToOne(mainPersistentClass, nestedPersistentClass, mainStatement,
                            nestedStatement,
                                preparedSql);
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
            PreparedSql<InsertStatement> preparedSql) throws SqlBuildException {

        String fieldName = Optional
                .ofNullable(nestedPersistentClass.getPropertyForRelClass(mainPersistentClass.getEntityClass()))
                .map(Property::fieldName)
                .orElseThrow(() -> new SqlBuildException("No relation property found"));

        FieldStatement relField = new FieldStatement();
        relField.setField(fieldName);
        relField.setValue(FOREIGN_KEY_PLACEHOLDER);
        nestedStatement.getFields().add(relField);

        preparedSql.addNestedSQLs(buildSql(nestedStatement));
    }

    /**
     * 处理多对一关系，嵌套插入主数据记录
     */
    private PreparedSql<InsertStatement> processManyToOne(
            PersistentClass mainPersistentClass,
            PersistentClass nestedPersistentClass,
            PersistStatement mainStatement,
            PersistStatement nestedStatement,
            PreparedSql<InsertStatement> preparedSql) throws SqlBuildException {

        // 判断内嵌对象包含id字段就调用processManyToOneIncludeId方法，否则调用processManyToOneWithoutId方法
        if (isReferenceOnlyStatement(nestedStatement)) {
            // 嵌套对象只包含ID，表示引用已存在的实体
            return processManyToOneIncludeId(mainPersistentClass, nestedPersistentClass, mainStatement,
                    nestedStatement, preparedSql);
        } else {
            // 嵌套对象包含其他字段，需要创建新的实体
            return processManyToOneWithoutId(mainPersistentClass, nestedPersistentClass, mainStatement,
                    nestedStatement, preparedSql);
        }
    }

    private PreparedSql<InsertStatement> processManyToOneIncludeId(
            PersistentClass mainPersistentClass,
            PersistentClass nestedPersistentClass,
            PersistStatement mainStatement,
            PersistStatement nestedStatement,
            PreparedSql<InsertStatement> preparedSql) throws SqlBuildException {

        // 内嵌的对象只有id，表示在insert时自动转换对应外键字段名，values就是传入的id的值

        // 从嵌套语句中提取ID值
        String idValue = extractIdFromNestedStatement(nestedStatement);
        if (StringUtils.isBlank(idValue)) {
            throw new SqlBuildException("在多对一关系中，嵌套实体的ID值不能为空");
        }

        // 获取外键字段名
        String foreignKeyFieldName = Optional
                .ofNullable(mainPersistentClass.getPropertyForRelClass(nestedPersistentClass.getEntityClass()))
                .map(Property::foreignKeyName)
                .orElse(nestedStatement.getEntityId().toLowerCase() + "_id");

        // 将外键字段添加到主实体语句中
        FieldStatement foreignKeyField = new FieldStatement();
        foreignKeyField.setField(foreignKeyFieldName);
        foreignKeyField.setValue(idValue);
        mainStatement.getFields().add(foreignKeyField);

        // 重新生成包含外键字段的SQL
        return buildSql(mainStatement);
    }

    private PreparedSql<InsertStatement> processManyToOneWithoutId(
            PersistentClass mainPersistentClass,
            PersistentClass nestedPersistentClass,
            PersistStatement mainStatement,
            PersistStatement nestedStatement,
            PreparedSql<InsertStatement> preparedSql) throws SqlBuildException {

        // 翻转 PersistentClass：多对一关系中，应该先插入"一"的一方
        PersistentClass tmpPersistentClass = mainPersistentClass;
        mainPersistentClass = nestedPersistentClass;
        nestedPersistentClass = tmpPersistentClass;

        // 翻转 Statement：对应地翻转语句
        PersistStatement tmpStatement = mainStatement;
        mainStatement = nestedStatement;
        nestedStatement = tmpStatement;

        // 先构建并添加"一"方的SQL（现在的 mainStatement）
        preparedSql = buildSql(mainStatement);

        // 在"多"方添加外键字段
        String foreignKeyFieldName = Optional
                .ofNullable(nestedPersistentClass.getPropertyForRelClass(mainPersistentClass.getEntityClass()))
                .map(Property::foreignKeyName)
                .orElse(mainStatement.getEntityId().toLowerCase() + "_id");

        FieldStatement foreignKeyField = new FieldStatement();
        foreignKeyField.setField(foreignKeyFieldName);
        foreignKeyField.setValue(FOREIGN_KEY_PLACEHOLDER);
        nestedStatement.getFields().add(foreignKeyField);

        // 构建并添加"多"方的SQL（现在的 nestedStatement）
        PreparedSql<InsertStatement> childSql = buildSql(nestedStatement);
        preparedSql.addNestedSQLs(childSql);

        return preparedSql;
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