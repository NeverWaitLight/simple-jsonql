package org.waitlight.simple.jsonql.engine;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
        if (mainSql.sql() != null && !mainSql.sql().isEmpty()) {
            allPreparedSqls.add(mainSql);
        }

        // 3. 处理嵌套实体
        for (Field nestedField : nestedFields) {
            allPreparedSqls.addAll(processNestedEntities(nestedField, createStatement.getEntityId(), extractParentId(createStatement)));
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
            return new PreparedSql<>("", List.of(), CreateStatement.class);
        }

        // 构建SQL语句
        StringBuilder sql = new StringBuilder("INSERT INTO ")
                .append(createStatement.getEntityId())
                .append(" (")
                .append(String.join(", ", fieldNames))
                .append(") VALUES (")
                .append(String.join(", ", java.util.Collections.nCopies(fieldNames.size(), "?")))
                .append(")");

        return new PreparedSql<>(sql.toString(), parameters, CreateStatement.class);
    }

    /**
     * 处理嵌套实体
     * @param field 包含嵌套实体的字段
     * @param parentEntityId 父实体ID
     * @param parentId 父记录ID
     * @return 嵌套实体的PreparedSql列表
     */
    private List<PreparedSql<CreateStatement>> processNestedEntities(Field field, String parentEntityId, Long parentId) {
        List<PreparedSql<CreateStatement>> nestedSqls = new ArrayList<>();
        log.info("Processing nested field: {}", field.getField());

        for (Object nestedValue : field.getValues()) {
            CreateStatement nestedCreate = convertToCreateStatement(nestedValue);
            if (nestedCreate == null) {
                continue;
            }

            String nestedEntityName = nestedCreate.getEntityId();
            if (nestedEntityName == null) {
                log.error("Could not determine entityId for a nested object within field '{}'. Skipping.",
                        field.getField());
                continue;
            }

            String foreignKeyFieldName = getForeignKeyFieldName(parentEntityId, nestedEntityName);
            if (foreignKeyFieldName == null) {
                log.error(
                        "Could not determine foreign key field name for relation {} -> {}. Skipping nested inserts for this object.",
                        parentEntityId, nestedEntityName);
                continue;
            }

            if (setupForeignKey(nestedCreate, foreignKeyFieldName, parentId)) {
                PreparedSql<CreateStatement> nestedSql = buildInsertSql(nestedCreate);
                if (nestedSql.sql() != null && !nestedSql.sql().isEmpty()) {
                    nestedSqls.add(nestedSql);
                }
            }
        }

        return nestedSqls;
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

    /**
     * 设置外键关系
     * @param nestedCreate 嵌套的创建语句
     * @param foreignKeyFieldName 外键字段名
     * @param parentId 父记录ID
     * @return 是否成功设置外键
     */
    private boolean setupForeignKey(CreateStatement nestedCreate, String foreignKeyFieldName, Long parentId) {
        if (nestedCreate.getFields() == null) {
            nestedCreate.setFields(new ArrayList<>());
        }

        // 检查是否已存在外键字段
        boolean fkFound = nestedCreate.getFields().stream()
                .anyMatch(f -> foreignKeyFieldName.equals(f.getField()));

        if (!fkFound && parentId != null) {
            // 添加外键字段
            Field foreignKeyField = new Field();
            foreignKeyField.setField(foreignKeyFieldName);
            foreignKeyField.setValue(parentId);
            nestedCreate.getFields().add(foreignKeyField);
            log.warn("Added missing foreign key field '{}' with value {} to nested entity of type '{}'",
                    foreignKeyFieldName, parentId, nestedCreate.getEntityId());
            return true;
        } else if (!fkFound && parentId == null) {
            log.error("Parent ID is null, cannot set foreign key '{}' for nested entity '{}'",
                    foreignKeyFieldName, nestedCreate.getEntityId());
            return false;
        }

        return true;
    }

    private Long extractParentId(CreateStatement createStatement) {
        Optional<Field> idField = createStatement.getFields().stream()
                .filter(f -> "id".equals(f.getField()))
                .findFirst();
        if (idField.isPresent() && idField.get().getValue() != null) {
            Object idValue = idField.get().getValue();
            if (idValue instanceof Number) {
                return ((Number) idValue).longValue();
            } else {
                try {
                    return Long.parseLong(idValue.toString());
                } catch (NumberFormatException e) {
                    log.error("Could not parse parent ID field '{}' with value '{}' as Long for entity '{}'.",
                            "id", idValue, createStatement.getEntityId(), e);
                    return null;
                }
            }
        }
        log.warn("Parent ID field 'id' not found or is null for entity: {}", createStatement.getEntityId());
        return null;
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
    protected Object doExecute(Connection conn, PreparedSql<?> sql) throws SQLException {
        if (!(sql.statementType() == CreateStatement.class)) {
            throw new IllegalArgumentException("CreateExecutor can only execute CreateStatements");
        }

        PreparedSql<CreateStatement> preparedCreate = (PreparedSql<CreateStatement>) sql;
        int totalAffectedRows = 0;

        // 执行插入
        if (preparedCreate.sql() != null && !preparedCreate.sql().isEmpty()) {
            try (PreparedStatement psParent = conn.prepareStatement(preparedCreate.sql())) {
                for (int i = 0; i < preparedCreate.parameters().size(); i++) {
                    psParent.setObject(i + 1, preparedCreate.parameters().get(i));
                }
                totalAffectedRows += psParent.executeUpdate();
            } catch (SQLException e) {
                log.error("Error executing insert SQL: {} with params {}: {}",
                        preparedCreate.sql(), preparedCreate.parameters(), e.getMessage(), e);
                throw e;
            }
        } else {
            log.warn("Insert SQL is empty, skipping execution.");
        }

        return totalAffectedRows;
    }
}