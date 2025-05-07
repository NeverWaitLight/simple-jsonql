package org.waitlight.simple.jsonql.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.waitlight.simple.jsonql.metadata.MetadataSources;
import org.waitlight.simple.jsonql.metadata.PersistentClass;
import org.waitlight.simple.jsonql.metadata.Property;
import org.waitlight.simple.jsonql.metadata.RelationshipType;
import org.waitlight.simple.jsonql.statement.CreateStatement;
import org.waitlight.simple.jsonql.statement.model.Field;
import org.waitlight.simple.jsonql.statement.model.JsonQLStatement;
import org.waitlight.simple.jsonql.statement.model.NestedEntity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Slf4j
public class CreateExecutor extends StatementExecutor<CreateStatement> {
    private static CreateExecutor instance;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private CreateExecutor(MetadataSources metadataSources) {
        super(metadataSources);
    }

    public static synchronized CreateExecutor getInstance(MetadataSources metadataSources) {
        if (instance == null) {
            instance = new CreateExecutor(metadataSources);
        }
        return instance;
    }

    @Override
    public Object execute(Connection conn, JsonQLStatement statement) throws SQLException {
        PreparedSql<CreateStatement> preparedSql = parseSql(statement);
        if (preparedSql.getSql() == null || preparedSql.getSql().isEmpty()) {
            return 0;
        }

        log.info("Execute create statement on entity: {}", ((CreateStatement) statement).getEntityId());
        log.info("Main SQL: {}", preparedSql.getSql());
        if (preparedSql.getParameters() != null && !preparedSql.getParameters().isEmpty()) {
            log.info("Main Parameters: {}", preparedSql.getParameters());
        }

        if (!preparedSql.getNestedSQLs().isEmpty()) {
            log.info("Create statement contains {} nested SQL statements", preparedSql.getNestedSQLs().size());
        }

        int totalAffectedRows = 0;
        boolean originalAutoCommit = conn.getAutoCommit();

        try {
            // 开启事务
            conn.setAutoCommit(false);

            // 1. 执行主实体插入
            Long generatedId = null;

            try (PreparedStatement ps = conn.prepareStatement(preparedSql.getSql(),
                    PreparedStatement.RETURN_GENERATED_KEYS)) {
                // 设置主实体的参数
                for (int i = 0; i < preparedSql.getParameters().size(); i++) {
                    ps.setObject(i + 1, preparedSql.getParameters().get(i));
                }

                int affected = ps.executeUpdate();
                totalAffectedRows += affected;

                // 获取生成的主键
                if (affected > 0) {
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            generatedId = rs.getLong(1);
                            log.info("Generated ID for main entity: {}", generatedId);
                        }
                    }
                }
            }

            // 2. 执行子实体插入
            if (generatedId != null && !preparedSql.getNestedSQLs().isEmpty()) {
                for (PreparedSql<CreateStatement> childSql : preparedSql.getNestedSQLs()) {
                    log.info("Executing nested SQL: {}", childSql.getSql());
                    if (childSql.getParameters() != null && !childSql.getParameters().isEmpty()) {
                        log.info("Nested Parameters (before ID replacement): {}", childSql.getParameters());
                    }

                    try (PreparedStatement ps = conn.prepareStatement(childSql.getSql())) {
                        List<Object> params = new ArrayList<>(childSql.getParameters());

                        // 更新外键参数值
                        for (int j = 0; j < params.size(); j++) {
                            Object param = params.get(j);
                            if (param instanceof ForeignKeyPlaceholder) {
                                params.set(j, generatedId);
                            }
                            ps.setObject(j + 1, params.get(j));
                        }

                        log.info("Nested Parameters (after ID replacement): {}", params);
                        totalAffectedRows += ps.executeUpdate();
                    }
                }
            }

            // 提交事务
            conn.commit();
            log.info("Transaction committed successfully, total affected rows: {}", totalAffectedRows);
            return totalAffectedRows;

        } catch (SQLException e) {
            // 发生异常时回滚事务
            try {
                conn.rollback();
                log.error("Transaction rolled back due to error: {}", e.getMessage());
            } catch (SQLException rollbackEx) {
                log.error("Error rolling back transaction", rollbackEx);
            }
            throw e;
        } finally {
            // 恢复自动提交设置
            try {
                conn.setAutoCommit(originalAutoCommit);
            } catch (SQLException e) {
                log.error("Error restoring autoCommit setting", e);
            }
        }
    }

    // 用于标记需要替换为生成的ID的参数
    private static class ForeignKeyPlaceholder {
        private final String fieldName;

        public ForeignKeyPlaceholder(String fieldName) {
            this.fieldName = fieldName;
        }
    }

    @Override
    protected PreparedSql<CreateStatement> parseSql(JsonQLStatement statement) {
        if (!(statement instanceof CreateStatement createStatement)) {
            throw new IllegalArgumentException(
                    "Expected CreateStatement but got " + statement.getClass().getSimpleName());
        }

        final String parentEntityId = createStatement.getEntityId();

        // 1. 处理主实体
        List<Field> directFields = createStatement.getFields().stream()
                .filter(field -> CollectionUtils.isEmpty(field.getValues()))
                .toList();
        List<Field> nestedFields = createStatement.getFields().stream()
                .filter(field -> CollectionUtils.isNotEmpty(field.getValues()))
                .toList();


        // 2. 生成主实体的SQL，创建一个新的CreateStatement避免修改原始对象
        CreateStatement parentCreateStatement = new CreateStatement();
        parentCreateStatement.setEntityId(parentEntityId);
        parentCreateStatement.setFields(directFields);

        PreparedSql<CreateStatement> preparedSql = buildSql(parentCreateStatement);

        // 3. 处理嵌套实体，使用ForeignKeyPlaceholder标记需要替换的外键
        for (Field nestedField : nestedFields) {
            for (NestedEntity nestedEntity : nestedField.getValues()) {

                final String childEntityId = nestedEntity.getEntityId();
                if (StringUtils.isBlank(childEntityId)) {
                    log.error("Could not determine entityId for a nested object within field '{}'. Skipping.", nestedField.getField());
                    continue;
                }

                String foreignKeyFieldName = getForeignKeyFieldName(parentEntityId, childEntityId);
                if (StringUtils.isBlank(foreignKeyFieldName)) {
                    log.error("Could not determine foreign key field name for relation {} -> {}. Skipping nested inserts for this object.",
                            createStatement.getEntityId(), childEntityId);
                    continue;
                }

                // 添加外键字段占位符
                Field foreignKeyField = new Field();
                foreignKeyField.setField(foreignKeyFieldName);
                foreignKeyField.setValue(new ForeignKeyPlaceholder(foreignKeyFieldName));
                nestedEntity.getFields().add(foreignKeyField);

                PreparedSql<CreateStatement> nestedSql = buildSql(nestedEntity);
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
    private PreparedSql<CreateStatement> buildSql(NestedEntity entity) {
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
     * @param parentEntityName 父实体名称
     * @param childEntityName  子实体名称
     * @return 外键字段名称，如果无法确定则返回null
     */
    private String getForeignKeyFieldName(String parentEntityName, String childEntityName) {
        PersistentClass parentEntityClass = metadata.getEntityBinding(parentEntityName);
        PersistentClass childEntityClass = metadata.getEntityBinding(childEntityName);

        if (ObjectUtils.anyNull(parentEntityClass, childEntityClass)) {
            log.warn("Entity not found in metadata: parent={}, child={}", parentEntityName, childEntityName);
            return null;
        }

        for (Property property : childEntityClass.getProperties()) {
            if (!RelationshipType.MANY_TO_ONE.equals(property.getRelationshipType())) {
                continue;
            }
            String foreignKeyName = property.getForeignKeyName();
            if (StringUtils.isNotBlank(foreignKeyName)) {
                return foreignKeyName;
            }
        }

        return parentEntityName.toLowerCase() + "_id";
    }

    @Override
    protected Object doExecute(Connection conn, PreparedSql<?> preparedSql) throws SQLException {
        if (preparedSql.getStatementType() != CreateStatement.class) {
            throw new IllegalArgumentException("CreateExecutor can only execute CreateStatements");
        }

        // 记录SQL语句和参数
        log.info("Executing SQL: {}", preparedSql.getSql());
        if (preparedSql.getParameters() != null && !preparedSql.getParameters().isEmpty()) {
            log.info("Parameters: {}", preparedSql.getParameters());
        }

        try (PreparedStatement ps = conn.prepareStatement(preparedSql.getSql(),
                PreparedStatement.RETURN_GENERATED_KEYS)) {
            // 设置参数
            List<Object> parameters = preparedSql.getParameters();
            for (int i = 0; i < parameters.size(); i++) {
                Object param = parameters.get(i);
                if (param instanceof ForeignKeyPlaceholder) {
                    throw new SQLException("Unexpected ForeignKeyPlaceholder in single SQL execution");
                }
                ps.setObject(i + 1, param);
            }

            // 执行插入
            int affectedRows = ps.executeUpdate();

            // 如果需要获取生成的主键，可以通过ResultSet rs = ps.getGeneratedKeys()获取
            // 但在这里我们只返回影响的行数
            return affectedRows;
        }
    }
}