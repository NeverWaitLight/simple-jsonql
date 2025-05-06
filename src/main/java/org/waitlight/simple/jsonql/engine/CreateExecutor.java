package org.waitlight.simple.jsonql.engine;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.waitlight.simple.jsonql.metadata.MetadataSources;
import org.waitlight.simple.jsonql.statement.CreateStatement;
import org.waitlight.simple.jsonql.statement.model.Field;
import org.waitlight.simple.jsonql.statement.model.JsonQLStatement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class CreateExecutor extends StatementExecutor {
    private static CreateExecutor instance;
    private final ObjectMapper objectMapper = new ObjectMapper(); // Jackson mapper

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
    protected PreparedSql<CreateStatement> parseSql(JsonQLStatement statement) {
        if (!(statement instanceof CreateStatement createStatement)) {
            throw new IllegalArgumentException(
                    "Expected CreateStatement but got " + statement.getClass().getSimpleName());
        }

        // 2. 提取父实体的 ID (假设它存在且为 Long 类型)
        Long parentId = extractParentId(createStatement);

        // 3. 准备父实体 SQL 和参数，并收集嵌套语句
        StringBuilder sql = new StringBuilder("INSERT INTO ")
                .append(createStatement.getEntityId())
                .append(" (");
        List<Object> parameters = new ArrayList<>();
        List<String> fieldNames = new ArrayList<>();
        List<CreateStatement> nestedStatements = new ArrayList<>();

        for (Field field : createStatement.getFields()) {
            // 如果字段包含 'values'，则认为是嵌套实体列表
            if (field.getValues() != null && !field.getValues().isEmpty()) {
                log.info("Processing nested field: {}", field.getField());

                for (Object nestedValue : field.getValues()) {
                    try {
                        // 将嵌套的 Map 转换为 CreateStatement 对象
                        Map<String, Object> nestedMap = objectMapper.convertValue(nestedValue,
                                new TypeReference<Map<String, Object>>() {
                                });
                        CreateStatement nestedCreate = objectMapper.convertValue(nestedMap, CreateStatement.class);

                        // 获取嵌套实体的名称
                        String nestedEntityName = nestedCreate.getEntityId();
                        if (nestedEntityName == null) {
                            log.error("Could not determine entityId for a nested object within field '{}'. Skipping.",
                                    field.getField());
                            continue; // 跳过这个无法识别的嵌套对象
                        }

                        String foreignKeyFieldName = getForeignKeyFieldName(createStatement.getEntityId(),
                                nestedEntityName);
                        if (foreignKeyFieldName == null) {
                            log.error(
                                    "Could not determine foreign key field name for relation {} -> {}. Skipping nested inserts for this object.",
                                    createStatement.getEntityId(), nestedEntityName);
                            continue;
                        }

                        // 注入父 ID
                        boolean fkFound = false;
                        if (nestedCreate.getFields() != null) {
                            for (Field nestedField : nestedCreate.getFields()) {
                                if (foreignKeyFieldName.equals(nestedField.getField())) {
                                    nestedField.setValue(parentId);
                                    fkFound = true;
                                    break;
                                }
                            }
                        } else {
                            nestedCreate.setFields(new ArrayList<>());
                        }

                        if (!fkFound && parentId != null) {
                            // 使用无参构造函数 + setter 方法
                            Field foreignKeyField = new Field();
                            foreignKeyField.setField(foreignKeyFieldName);
                            foreignKeyField.setValue(parentId);
                            nestedCreate.getFields().add(foreignKeyField);
                            log.warn("Added missing foreign key field '{}' with value {} to nested entity of type '{}'",
                                    foreignKeyFieldName, parentId, nestedCreate.getEntityId());
                        } else if (!fkFound && parentId == null) {
                            log.error("Parent ID is null, cannot set foreign key '{}' for nested entity '{}'",
                                    foreignKeyFieldName, nestedCreate.getEntityId());
                            continue;
                        }

                        nestedStatements.add(nestedCreate);

                    } catch (IllegalArgumentException e) {
                        log.error("Error converting nested value for field '{}' to CreateStatement: {}",
                                field.getField(), e.getMessage(), e);
                    }
                }
            } else {
                // 普通字段，添加到父实体的 SQL 中
                fieldNames.add(field.getField());
                parameters.add(field.getValue());
            }
        }

        // 4. 完成父实体 SQL
        if (fieldNames.isEmpty()) {
            log.warn("Create statement for entity '{}' has no direct fields to insert.", createStatement.getEntityId());
            return new PreparedSql<>("", List.of(), nestedStatements, CreateStatement.class);
        }

        sql.append(String.join(", ", fieldNames)).append(") VALUES (");
        sql.append(String.join(", ", java.util.Collections.nCopies(fieldNames.size(), "?")));
        sql.append(")");

        // 5. 返回 PreparedSql 对象，包含父 SQL 和嵌套语句列表
        return new PreparedSql<>(sql.toString(), parameters, nestedStatements, CreateStatement.class);
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
                    // 尝试将字符串等类型转换为 Long
                    return Long.parseLong(idValue.toString());
                } catch (NumberFormatException e) {
                    log.error("Could not parse parent ID field '{}' with value '{}' as Long for entity '{}'.",
                            "id", idValue, createStatement.getEntityId(), e);
                    return null; // 或者抛出异常
                }
            }
        }
        log.warn("Parent ID field 'id' not found or is null for entity: {}", createStatement.getEntityId());
        return null; // 如果找不到 ID 或 ID 为 null
    }

    // TODO: 实现基于元数据的外键查找逻辑
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

    // doExecute 方法签名也需要调整
    @Override
    protected Object doExecute(Connection conn, PreparedSql<?> sql) throws SQLException {
        // 下一步实现这个方法
        if (!(sql.statementType() == CreateStatement.class)) {
            throw new IllegalArgumentException("CreateExecutor can only execute CreateStatements");
        }

        PreparedSql<CreateStatement> preparedCreate = (PreparedSql<CreateStatement>) sql;
        int totalAffectedRows = 0;

        // 1. 执行父实体插入 (如果 SQL 存在)
        if (preparedCreate.sql() != null && !preparedCreate.sql().isEmpty()) {
            try (PreparedStatement psParent = conn.prepareStatement(preparedCreate.sql())) {
                for (int i = 0; i < preparedCreate.parameters().size(); i++) {
                    psParent.setObject(i + 1, preparedCreate.parameters().get(i));
                }
                totalAffectedRows += psParent.executeUpdate();
            } catch (SQLException e) {
                log.error("Error executing parent insert SQL: {} with params {}: {}",
                        preparedCreate.sql(), preparedCreate.parameters(), e.getMessage(), e);
                throw e; // 重新抛出异常，事务应回滚
            }
        } else {
            log.warn("Parent insert SQL is empty, skipping execution.");
        }

        // 2. 递归执行嵌套实体插入
        if (!preparedCreate.nestedCreateStatements().isEmpty()) {
            log.info("Executing {} nested create statements...", preparedCreate.nestedCreateStatements().size());
            for (CreateStatement nestedStatement : preparedCreate.nestedCreateStatements()) {
                try {
                    // 注意：这里假设嵌套层级不深，直接递归调用 execute
                    // 在复杂场景下可能需要更精细的事务和连接管理
                    // 同时，确保 execute 方法是线程安全的或在此上下文中安全调用
                    Object nestedResult = this.execute(conn, nestedStatement); // 使用当前实例和连接
                    if (nestedResult instanceof Integer) {
                        totalAffectedRows += (Integer) nestedResult;
                    } else {
                        log.warn("Nested create statement execution returned non-integer result: {}", nestedResult);
                        // 根据需要决定是否增加计数或抛出错误
                        // 如果嵌套本身还有嵌套，这里需要处理返回的 PreparedSql 而不是 Integer
                        // 为了简化，当前假设 execute(conn, CreateStatement) 返回影响行数
                    }
                } catch (Exception e) { // 捕获 SQLException 和其他可能的异常
                    log.error("Error executing nested create statement for entity '{}': {}",
                            nestedStatement.getEntityId(), e.getMessage(), e);
                    // 决定是继续执行其他嵌套语句还是抛出异常中断整个过程
                    throw new SQLException(
                            "Failed to execute nested create for entity: " + nestedStatement.getEntityId(), e);
                }
            }
        }

        return totalAffectedRows;
    }
}