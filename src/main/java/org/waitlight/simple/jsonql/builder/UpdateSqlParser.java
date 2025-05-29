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
import org.waitlight.simple.jsonql.statement.StatementUtils;
import org.waitlight.simple.jsonql.statement.StatementsPairs;
import org.waitlight.simple.jsonql.statement.UpdateStatement;
import org.waitlight.simple.jsonql.statement.model.FieldStatement;
import org.waitlight.simple.jsonql.statement.model.PersistStatement;

import java.util.*;

public class UpdateSqlParser extends AbstractPersistSqlBuilder<UpdateStatement> {

    private static final Logger log = LoggerFactory.getLogger(UpdateSqlParser.class);

    public UpdateSqlParser(Metadata metadata) {
        super(metadata);
    }

    @Override
    public PreparedSql<UpdateStatement> build(UpdateStatement statement) throws SqlBuildException {
        if (Objects.isNull(statement)) {
            throw new SqlBuildException("Statement is null");
        }
        final String mainEntityId = statement.getEntityId();

        final PersistentClass mainPersistentClass = metadata.getEntity(mainEntityId);
        final StatementsPairs<PersistStatement> statementsPairs = StatementUtils.convert2SingleLevel(statement);

        final PersistStatement mainStatement = statementsPairs.mainStatement();
        PreparedSql<UpdateStatement> preparedSql = buildSql(mainStatement);

        for (PersistStatement nestedStatement : statementsPairs.nestedStatements()) {
            String nestedEntityId = nestedStatement.getEntityId();
            PersistentClass nestedPersistentClass = metadata.getEntity(nestedEntityId);
            RelationshipType relationshipType = mainPersistentClass.getRelations()
                    .get(nestedPersistentClass.getEntityClass());
            switch (relationshipType) {
                case ONE_TO_MANY ->
                        preparedSql = processOneToMany(mainPersistentClass, nestedPersistentClass, mainStatement,
                                nestedStatement, preparedSql);
                case MANY_TO_ONE ->
                        preparedSql = processManyToOne(mainPersistentClass, nestedPersistentClass, mainStatement,
                                nestedStatement, preparedSql);
                default -> {

                }
            }
        }

        return preparedSql;
    }

    /**
     * 处理一对多关系，根据嵌套对象是否包含ID来决定处理方式
     */
    private PreparedSql<UpdateStatement> processOneToMany(
            PersistentClass mainPersistentClass,
            PersistentClass nestedPersistentClass,
            PersistStatement mainStatement,
            PersistStatement nestedStatement,
            PreparedSql<UpdateStatement> preparedSql) throws SqlBuildException {

        String fieldName = Optional
                .ofNullable(nestedPersistentClass.getPropertyForRelClass(mainPersistentClass.getEntityClass()))
                .map(Property::fieldName)
                .orElseThrow(() -> new SqlBuildException("No relation property found"));

        FieldStatement relField = new FieldStatement();
        relField.setField(fieldName);
        relField.setValue(mainStatement.getDataId());
        nestedStatement.getFields().add(relField);

        preparedSql.addNestedSQLs(buildSql(nestedStatement));

        return preparedSql;
    }

    /**
     * 处理多对一关系，根据嵌套对象是否包含ID来决定处理方式
     */
    private PreparedSql<UpdateStatement> processManyToOne(
            PersistentClass mainPersistentClass,
            PersistentClass nestedPersistentClass,
            PersistStatement mainStatement,
            PersistStatement nestedStatement,
            PreparedSql<UpdateStatement> preparedSql) throws SqlBuildException {

        if (isOnlyIncludeId(nestedStatement)) {
            String idValue = extractNestedId(nestedStatement);
            if (StringUtils.isBlank(idValue)) {
                throw new SqlBuildException("在多对一关系中，嵌套实体的ID值不能为空");
            }

            String foreignKeyFieldName = Optional
                    .ofNullable(mainPersistentClass.getPropertyForRelClass(nestedPersistentClass.getEntityClass()))
                    .map(Property::foreignKeyName)
                    .orElse(nestedStatement.getEntityId().toLowerCase() + "_id");

            FieldStatement foreignKeyField = new FieldStatement();
            foreignKeyField.setField(foreignKeyFieldName);
            foreignKeyField.setValue(idValue);
            mainStatement.getFields().add(foreignKeyField);

            return buildSql(mainStatement);
        } else {
            log.warn("更新操作中不支持创建新的嵌套实体，只能引用已存在的实体ID");
            return preparedSql;
        }
    }

    /**
     * 判断嵌套语句是否仅包含ID字段
     */
    private boolean isOnlyIncludeId(PersistStatement stmt) {
        if (StringUtils.isNotBlank(stmt.getDataId())) {
            return true;
        }

        if (CollectionUtils.isEmpty(stmt.getFields())) {
            return false;
        }

        if (stmt.getFields().size() == 1) {
            FieldStatement field = stmt.getFields().getFirst();
            return "id".equals(field.getField());
        }

        return false;
    }

    /**
     * 从嵌套语句中提取ID值
     */
    private String extractNestedId(PersistStatement stmt) {
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
     * 构建SQL语句及其参数
     */
    private PreparedSql<UpdateStatement> buildSql(PersistStatement statement) throws SqlBuildException {
        if (Objects.isNull(statement) || CollectionUtils.isEmpty(statement.getFields())) {
            return new PreparedSql<>();
        }

        if (StringUtils.isBlank(statement.getDataId())) {
            throw new SqlBuildException("UPDATE语句必须提供dataId作为WHERE条件");
        }

        DSLContext create = DSL.using(SQLDialect.MYSQL);
        Table<?> table = DSL.table(DSL.name(statement.getEntityId()));

        List<Field<Object>> updateFields = new ArrayList<>();
        List<Object> updateValues = new ArrayList<>();
        List<Object> parameters = new ArrayList<>();

        Map<FieldStatement, Property> map = map(statement.getEntityId(), statement);
        for (FieldStatement field : statement.getFields()) {
            Property property = map.get(field);
            String columnName = property.columnName();
            updateFields.add(DSL.field(DSL.name(columnName)));
            updateValues.add(field.getValue());
            parameters.add(field.getValue());
        }

        // 构建UPDATE语句
        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE `").append(statement.getEntityId()).append("` SET ");

        List<String> setClauses = new ArrayList<>();
        for (int i = 0; i < updateFields.size(); i++) {
            setClauses.add(updateFields.get(i).getName() + " = ?");
        }
        sql.append(String.join(", ", setClauses));

        sql.append(" WHERE `id` = ?");
        parameters.add(statement.getDataId());

        log.info("build update sql: {}", sql);
        return new PreparedSql<>(sql.toString(), parameters, UpdateStatement.class);
    }
}