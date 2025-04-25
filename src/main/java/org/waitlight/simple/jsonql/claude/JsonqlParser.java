package org.waitlight.simple.jsonql.claude;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JSONQL解析器，将JSON格式的查询转换为ORM查询
 */
@Component
public class JsonqlParser {
    private final CustomEntityManager entityManager;
    private final ObjectMapper objectMapper;
    private final Map<String, Class<?>> entityRegistry = new ConcurrentHashMap<>();

    public JsonqlParser(CustomEntityManager entityManager, ObjectMapper objectMapper) {
        this.entityManager = entityManager;
        this.objectMapper = objectMapper;
    }

    /**
     * 注册实体类
     */
    public void registerEntity(String entityName, Class<?> entityClass) {
        entityRegistry.put(entityName, entityClass);
    }

    /**
     * 解析JSONQL查询字符串
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> execute(String jsonQueryStr) {
        try {
            JsonNode jsonQuery = objectMapper.readTree(jsonQueryStr);
            return (List<T>) execute(jsonQuery);
        } catch (Exception e) {
            throw new JsonqlException("解析JSONQL查询失败", e);
        }
    }

    /**
     * 解析JSONQL查询对象
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> execute(JsonNode jsonQuery) {
        // 验证必需的属性
        if (!jsonQuery.has("from")) {
            throw new JsonqlException("JSONQL查询必须指定 'from' 属性");
        }

        // 获取实体类
        String entityName = jsonQuery.get("from").asText();
        Class<T> entityClass = (Class<T>) getEntityClassByName(entityName);

        // 创建查询构建器
        QueryBuilder<T> queryBuilder = entityManager.createQuery(entityClass);

        // 处理where条件
        if (jsonQuery.has("where")) {
            parseWhereConditions(queryBuilder, jsonQuery.get("where"));
        }

        // 处理排序
        if (jsonQuery.has("orderBy")) {
            parseOrderBy(queryBuilder, jsonQuery.get("orderBy"));
        }

        // 处理分页
        if (jsonQuery.has("limit")) {
            queryBuilder.limit(jsonQuery.get("limit").asInt());
        }

        if (jsonQuery.has("offset")) {
            queryBuilder.offset(jsonQuery.get("offset").asInt());
        }

        // 处理关联查询
        if (jsonQuery.has("include")) {
            parseIncludes(queryBuilder, jsonQuery.get("include"));
        }

        // 执行查询
        return queryBuilder.getResults();
    }

    /**
     * 解析条件表达式
     */
    private <T> void parseWhereConditions(QueryBuilder<T> queryBuilder, JsonNode whereConditions) {
        List<String> conditions = new ArrayList<>();
        List<Object> parameters = new ArrayList<>();

        Iterator<Map.Entry<String, JsonNode>> fields = whereConditions.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String field = entry.getKey();
            JsonNode condition = entry.getValue();

            // 处理简单等值条件
            if (condition.isValueNode()) {
                conditions.add("e." + field + " = ?");
                parameters.add(getValueFromJsonNode(condition));
                continue;
            }

            // 处理操作符条件
            Iterator<Map.Entry<String, JsonNode>> operators = condition.fields();
            while (operators.hasNext()) {
                Map.Entry<String, JsonNode> op = operators.next();
                String operator = op.getKey();
                JsonNode value = op.getValue();

                switch (operator) {
                    case "$eq" -> {
                        conditions.add("e." + field + " = ?");
                        parameters.add(getValueFromJsonNode(value));
                    }
                    case "$ne" -> {
                        conditions.add("e." + field + " != ?");
                        parameters.add(getValueFromJsonNode(value));
                    }
                    case "$gt" -> {
                        conditions.add("e." + field + " > ?");
                        parameters.add(getValueFromJsonNode(value));
                    }
                    case "$gte" -> {
                        conditions.add("e." + field + " >= ?");
                        parameters.add(getValueFromJsonNode(value));
                    }
                    case "$lt" -> {
                        conditions.add("e." + field + " < ?");
                        parameters.add(getValueFromJsonNode(value));
                    }
                    case "$lte" -> {
                        conditions.add("e." + field + " <= ?");
                        parameters.add(getValueFromJsonNode(value));
                    }
                    case "$like" -> {
                        conditions.add("e." + field + " LIKE ?");
                        parameters.add(getValueFromJsonNode(value));
                    }
                    case "$in" -> {
                        if (value.isArray() && value.size() > 0) {
                            List<Object> inValues = new ArrayList<>();
                            for (JsonNode item : value) {
                                inValues.add(getValueFromJsonNode(item));
                            }
                            conditions.add("e." + field + " IN ?");
                            parameters.add(inValues);
                        }
                    }
                    default -> throw new JsonqlException("不支持的操作符: " + operator);
                }
            }
        }

        // 将所有条件添加到查询构建器
        if (!conditions.isEmpty()) {
            StringBuilder whereClause = new StringBuilder();
            for (int i = 0; i < conditions.size(); i++) {
                if (i > 0) {
                    whereClause.append(" AND ");
                }
                whereClause.append(conditions.get(i));
            }
            queryBuilder.where(whereClause.toString(), parameters.toArray());
        }
    }

    /**
     * 解析排序
     */
    private <T> void parseOrderBy(QueryBuilder<T> queryBuilder, JsonNode orderBy) {
        Iterator<Map.Entry<String, JsonNode>> fields = orderBy.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String field = entry.getKey();
            String direction = entry.getValue().asText().toUpperCase();
            queryBuilder.orderBy(field, direction);
        }
    }

    /**
     * 解析关联查询
     */
    private <T> void parseIncludes(QueryBuilder<T> queryBuilder, JsonNode includes) {
        if (includes.isArray()) {
            for (JsonNode include : includes) {
                if (include.isObject() && include.has("relation")) {
                    String relation = include.get("relation").asText();
                    queryBuilder.include(relation);

                    // TODO: 关联查询的其他条件可以在后续的关联数据装配中处理
                } else if (include.isTextual()) {
                    queryBuilder.include(include.asText());
                }
            }
        }
    }

    /**
     * 从JsonNode获取值
     */
    private Object getValueFromJsonNode(JsonNode node) {
        if (node.isNull()) return null;
        if (node.isTextual()) return node.asText();
        if (node.isInt()) return node.asInt();
        if (node.isLong()) return node.asLong();
        if (node.isDouble()) return node.asDouble();
        if (node.isBoolean()) return node.asBoolean();

        return node.toString();
    }

    /**
     * 根据实体名称获取实体类
     */
    private Class<?> getEntityClassByName(String entityName) {
        Class<?> entityClass = entityRegistry.get(entityName);

        if (entityClass == null) {
            throw new JsonqlException("未找到实体类: " + entityName);
        }

        return entityClass;
    }

    /**
     * JSONQL异常
     */
    public static class JsonqlException extends RuntimeException {
        public JsonqlException(String message) {
            super(message);
        }

        public JsonqlException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}