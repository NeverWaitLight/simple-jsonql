package org.waitlight.simple.jsonql.statement;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.waitlight.simple.jsonql.statement.model.FilterCriteria;
import org.waitlight.simple.jsonql.statement.model.PageCriteria;
import org.waitlight.simple.jsonql.statement.model.SortCriteria;
import org.waitlight.simple.jsonql.statement.model.StatementType;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class StatementParser {
    private final ObjectMapper objectMapper;

    public StatementParser() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 将JSON字符串解析为指定类型的JsonQLStatement对象
     *
     * @param jsonQL              JSON字符串
     * @param jsonQLStatementType 目标JsonQLStatement类型
     * @return 指定类型的JsonQLStatement对象
     * @throws JsonQLStatementException 解析异常
     */
    public <T extends JsonQLStatement> T parse(String jsonQL, Class<T> jsonQLStatementType) throws JsonQLStatementException {
        if (StringUtils.isBlank(jsonQL)) {
            throw new JsonQLStatementException("JsonQL cannot be empty");
        }
        if (Objects.isNull(jsonQLStatementType)) {
            throw new JsonQLStatementException("JsonQLStatement type cannot be null");
        }

        T statement;
        try {
            statement = objectMapper.readValue(jsonQL, jsonQLStatementType);
        } catch (Exception e) {
            throw new JsonQLStatementException(
                    "Failed to parse JsonQL to " + jsonQLStatementType.getSimpleName() + ": " + e.getMessage(), e);
        }

        return statement;

    }

    public JsonQLStatement parse(String jsonQL) throws JsonQLStatementException {
        Map<String, Object> jsonMap;
        try {
            jsonMap = objectMapper.readValue(jsonQL, Map.class);
        } catch (Exception e) {
            throw new JsonQLStatementException("Failed to parse JsonQL: " + e.getMessage(), e);
        }

        String statementStr = (String) jsonMap.get("statement");
        if (statementStr == null) {
            throw new JsonQLStatementException("Statement type is required");
        }

        StatementType statementType;
        try {
            statementType = StatementType.fromValue(statementStr);
        } catch (IllegalArgumentException e) {
            throw new JsonQLStatementException("Unsupported statement type: " + statementStr);
        }

        return switch (statementType) {
            case SELECT -> parseQuery(jsonMap);
            case INSERT -> parseCreate(jsonMap);
            case UPDATE -> parseUpdate(jsonMap);
            case DELETE -> parseDelete(jsonMap);
            default -> throw new JsonQLStatementException("Unsupported statement type: " + statementType);
        };
    }

    private SelectStatement parseQuery(Map<String, Object> jsonMap) throws JsonQLStatementException {
        SelectStatement statement = new SelectStatement();

        // 设置CRUD.md中定义的字段
        if (jsonMap.containsKey("appId")) {
            statement.setAppId((String) jsonMap.get("appId"));
        }

        if (jsonMap.containsKey("formId")) {
            statement.setFormId((String) jsonMap.get("formId"));
        }

        if (jsonMap.containsKey("entityId")) {
            statement.setEntityId((String) jsonMap.get("entityId"));
        }

        if (jsonMap.containsKey("filters")) {
            try {
                Map<String, Object> filtersMap = (Map<String, Object>) jsonMap.get("filters");
                String json = objectMapper.writeValueAsString(filtersMap);
                FilterCriteria filters = objectMapper.readValue(json, FilterCriteria.class);
                statement.setFilters(filters);
            } catch (Exception e) {
                throw new JsonQLStatementException("Failed to parse filters: " + e.getMessage(), e);
            }
        }

        if (jsonMap.containsKey("sort")) {
            try {
                List<Map<String, Object>> sortList = (List<Map<String, Object>>) jsonMap.get("sort");
                String json = objectMapper.writeValueAsString(sortList);
                List<SortCriteria> sort = objectMapper.readValue(json,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, SortCriteria.class));
                statement.setSort(sort);
            } catch (Exception e) {
                throw new JsonQLStatementException("Failed to parse sort: " + e.getMessage(), e);
            }
        }

        if (jsonMap.containsKey("page")) {
            try {
                Map<String, Object> pageMap = (Map<String, Object>) jsonMap.get("page");
                String json = objectMapper.writeValueAsString(pageMap);
                PageCriteria page = objectMapper.readValue(json, PageCriteria.class);
                statement.setPage(page);
            } catch (Exception e) {
                throw new JsonQLStatementException("Failed to parse page: " + e.getMessage(), e);
            }
        }

        return statement;
    }

    private InsertStatement parseCreate(Map<String, Object> jsonMap) throws JsonQLStatementException {
        try {
            // Remove the statement field as it's already processed
            jsonMap.remove("statement");

            // Convert the Map to JSON and then to CreateStatement
            String json = objectMapper.writeValueAsString(jsonMap);
            InsertStatement statement = objectMapper.readValue(json, InsertStatement.class);

            // Set the statement type

            return statement;
        } catch (Exception e) {
            if (e instanceof JsonQLStatementException) {
                throw (JsonQLStatementException) e;
            }
            throw new JsonQLStatementException("Failed to parse CREATE statement: " + e.getMessage(), e);
        }
    }

    private UpdateStatement parseUpdate(Map<String, Object> jsonMap) throws JsonQLStatementException {
        try {
            // Remove the statement field as it's already processed
            jsonMap.remove("statement");

            // Convert the Map to JSON and then to UpdateStatement
            String json = objectMapper.writeValueAsString(jsonMap);
            UpdateStatement statement = objectMapper.readValue(json, UpdateStatement.class);

            // Set the statement type

            return statement;
        } catch (Exception e) {
            if (e instanceof JsonQLStatementException) {
                throw (JsonQLStatementException) e;
            }
            throw new JsonQLStatementException("Failed to parse UPDATE statement: " + e.getMessage(), e);
        }
    }

    private DeleteStatement parseDelete(Map<String, Object> jsonMap) throws JsonQLStatementException {
        try {
            // Remove the statement field as it's already processed
            jsonMap.remove("statement");

            // Convert the Map to JSON and then to DeleteStatement
            String json = objectMapper.writeValueAsString(jsonMap);
            DeleteStatement statement = objectMapper.readValue(json, DeleteStatement.class);

            // Set the statement type

            return statement;
        } catch (Exception e) {
            if (e instanceof JsonQLStatementException) {
                throw (JsonQLStatementException) e;
            }
            throw new JsonQLStatementException("Failed to parse DELETE statement: " + e.getMessage(), e);
        }
    }
} 