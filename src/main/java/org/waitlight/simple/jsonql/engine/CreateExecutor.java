package org.waitlight.simple.jsonql.engine;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.waitlight.simple.jsonql.metadata.MetadataSources;
import org.waitlight.simple.jsonql.statement.CreateStatement;
import org.waitlight.simple.jsonql.statement.model.Field;
import org.waitlight.simple.jsonql.statement.model.JsonQLStatement;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

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
        List<PreparedSql<CreateStatement>> preparedSqls = parseSql(statement);
        if (preparedSqls.isEmpty()) {
            return 0;
        }

        int totalAffectedRows = 0;
        boolean originalAutoCommit = conn.getAutoCommit();

        try {
            // 开启事务
            conn.setAutoCommit(false);

            // 1. 执行主实体插入
            PreparedSql<CreateStatement> mainSql = preparedSqls.get(0);
            Long generatedId = null;

            try (PreparedStatement ps = conn.prepareStatement(mainSql.getSql(), PreparedStatement.RETURN_GENERATED_KEYS)) {
                // 设置主实体的参数
                for (int i = 0; i < mainSql.getParameters().size(); i++) {
                    ps.setObject(i + 1, mainSql.getParameters().get(i));
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
            if (generatedId != null && preparedSqls.size() > 1) {
                for (int i = 1; i < preparedSqls.size(); i++) {
                    PreparedSql<CreateStatement> childSql = preparedSqls.get(i);
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

                        totalAffectedRows += ps.executeUpdate();
                    }
                }
            }

            // 提交事务
            conn.commit();
            return totalAffectedRows;

        } catch (SQLException e) {
            // 发生异常时回滚事务
            try {
                conn.rollback();
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
    protected List<PreparedSql<CreateStatement>> parseSql(JsonQLStatement statement) {
        if (!(statement instanceof CreateStatement createStatement)) {
            throw new IllegalArgumentException(
                    "Expected CreateStatement but got " + statement.getClass().getSimpleName());
        }

        List<PreparedSql<CreateStatement>> allPreparedSqls = new ArrayList<>();

        // 1. 处理主实体
        List<Field> directFields = new ArrayList<>();
        List<Field> nestedFields = new ArrayList<>();

        // 分离直接字段和嵌套字段
        for (Field field : createStatement.getFields()) {
            if (field.getValues() != null && !field.getValues().isEmpty()) {
                nestedFields.add(field);
            } else {
                directFields.add(field);
            }
        }

        // 2. 生成主实体的SQL
        createStatement.setFields(directFields);
        PreparedSql<CreateStatement> mainSql = buildInsertSql(createStatement);
        if (mainSql.getSql() != null && !mainSql.getSql().isEmpty()) {
            allPreparedSqls.add(mainSql);
        }

        // 3. 处理嵌套实体，使用ForeignKeyPlaceholder标记需要替换的外键
        for (Field nestedField : nestedFields) {
            for (Object nestedValue : nestedField.getValues()) {
                CreateStatement nestedCreate = convertToCreateStatement(nestedValue);
                if (nestedCreate == null) {
                    continue;
                }

                String nestedEntityName = nestedCreate.getEntityId();
                if (nestedEntityName == null) {
                    log.error("Could not determine entityId for a nested object within field '{}'. Skipping.",
                            nestedField.getField());
                    continue;
                }

                String foreignKeyFieldName = getForeignKeyFieldName(createStatement.getEntityId(), nestedEntityName);
                if (foreignKeyFieldName == null) {
                    log.error(
                            "Could not determine foreign key field name for relation {} -> {}. Skipping nested inserts for this object.",
                            createStatement.getEntityId(), nestedEntityName);
                    continue;
                }

                // 添加外键字段占位符
                Field foreignKeyField = new Field();
                foreignKeyField.setField(foreignKeyFieldName);
                foreignKeyField.setValue(new ForeignKeyPlaceholder(foreignKeyFieldName));
                nestedCreate.getFields().add(foreignKeyField);

                PreparedSql<CreateStatement> nestedSql = buildInsertSql(nestedCreate);
                if (nestedSql.getSql() != null && !nestedSql.getSql().isEmpty()) {
                    allPreparedSqls.add(nestedSql);
                }
            }
        }

        return allPreparedSqls;
    }

    /**
     * 构建INSERT SQL语句
     * @param createStatement 创建语句
     * @return PreparedSql对象
     */
    private PreparedSql<CreateStatement> buildInsertSql(CreateStatement createStatement) {
        List<String> fieldNames = new ArrayList<>();
        List<Object> parameters = new ArrayList<>();

        // 收集字段名和参数值
        for (Field field : createStatement.getFields()) {
            fieldNames.add(field.getField());
            parameters.add(field.getValue());
        }

        // 如果没有直接字段要插入，返回空SQL
        if (fieldNames.isEmpty()) {
            log.warn("Create statement for entity '{}' has no direct fields to insert.", createStatement.getEntityId());
            PreparedSql<CreateStatement> result = new PreparedSql<>();
            result.setSql("");
            result.setParameters(List.of());
            result.setStatementType(CreateStatement.class);
            return result;
        }

        // 构建SQL语句
        StringBuilder sql = new StringBuilder("INSERT INTO ")
                .append(createStatement.getEntityId())
                .append(" (")
                .append(String.join(", ", fieldNames))
                .append(") VALUES (")
                .append(String.join(", ", java.util.Collections.nCopies(fieldNames.size(), "?")))
                .append(")");

        PreparedSql<CreateStatement> result = new PreparedSql<>();
        result.setSql(sql.toString());
        result.setParameters(parameters);
        result.setStatementType(CreateStatement.class);
        return result;
    }

    /**
     * 将嵌套对象转换为CreateStatement
     * @param nestedValue 嵌套对象值
     * @return CreateStatement对象，如果转换失败返回null
     */
    private CreateStatement convertToCreateStatement(Object nestedValue) {
        try {
            Map<String, Object> nestedMap = objectMapper.convertValue(nestedValue,
                    new TypeReference<Map<String, Object>>() {});
            return objectMapper.convertValue(nestedMap, CreateStatement.class);
        } catch (IllegalArgumentException e) {
            log.error("Failed to convert nested value to CreateStatement: {}", e.getMessage());
            return null;
        }
    }

    private String getForeignKeyFieldName(String parentEntityName, String childEntityName) {
        // 这个方法需要查询你的元数据 (MetadataSources/Metadata)
        // 来确定 childEntityName 中引用 parentEntityName 的外键字段名
        // 例如，对于 parent="user", child="blog"，它应该返回 "user_id"

        // 临时硬编码实现 - 需要替换为真实的元数据查找
        if ("user".equals(parentEntityName) && "blog".equals(childEntityName)) {
            return "user_id";
        }
        log.warn("Metadata lookup for foreign key from {} to {} not implemented yet. Returning null.", parentEntityName,
                childEntityName);
        return null;
    }

    @Override
    protected Object doExecute(Connection conn, PreparedSql<?> preparedSql) throws SQLException {
        if (preparedSql.getStatementType() != CreateStatement.class) {
            throw new IllegalArgumentException("CreateExecutor can only execute CreateStatements");
        }

        try (PreparedStatement ps = conn.prepareStatement(preparedSql.getSql(), PreparedStatement.RETURN_GENERATED_KEYS)) {
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