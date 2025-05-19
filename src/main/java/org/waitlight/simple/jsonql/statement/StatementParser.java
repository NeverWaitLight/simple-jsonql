package org.waitlight.simple.jsonql.statement;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.waitlight.simple.jsonql.statement.model.FilterCriteria;
import org.waitlight.simple.jsonql.statement.model.PageCriteria;
import org.waitlight.simple.jsonql.statement.model.SortCriteria;
import org.waitlight.simple.jsonql.statement.model.StatementType;

import java.util.List;
import java.util.Map;

public class StatementParser {
    private final ObjectMapper objectMapper;

    public StatementParser() {
        this.objectMapper = new ObjectMapper();
    }

    public JsonQLStatement parse2Stmt(String jsonQL) throws JsonqlParseException {
        try {
            Map<String, Object> jsonMap = objectMapper.readValue(jsonQL, Map.class);
            String statementStr = (String) jsonMap.get("statement");
            if (statementStr == null) {
                throw new JsonqlParseException("Statement type is required");
            }

            StatementType statementType;
            try {
                statementType = StatementType.fromValue(statementStr);
            } catch (IllegalArgumentException e) {
                throw new JsonqlParseException("Unsupported statement type: " + statementStr);
            }

            return switch (statementType) {
                case SELECT -> parseQuery(jsonMap);
                case INSERT -> parseCreate(jsonMap);
                case UPDATE -> parseUpdate(jsonMap);
                case DELETE -> parseDelete(jsonMap);
                default -> throw new JsonqlParseException("Unsupported statement type: " + statementType);
            };
        } catch (Exception e) {
            if (e instanceof JsonqlParseException) {
                throw (JsonqlParseException) e;
            }
            throw new JsonqlParseException("Failed to parse JSONQL: " + e.getMessage(), e);
        }
    }

    private SelectStatement parseQuery(Map<String, Object> jsonMap) throws JsonqlParseException {
        SelectStatement statement = new SelectStatement();
        statement.setStatement(StatementType.SELECT);

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
                throw new JsonqlParseException("Failed to parse filters: " + e.getMessage(), e);
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
                throw new JsonqlParseException("Failed to parse sort: " + e.getMessage(), e);
            }
        }

        if (jsonMap.containsKey("page")) {
            try {
                Map<String, Object> pageMap = (Map<String, Object>) jsonMap.get("page");
                String json = objectMapper.writeValueAsString(pageMap);
                PageCriteria page = objectMapper.readValue(json, PageCriteria.class);
                statement.setPage(page);
            } catch (Exception e) {
                throw new JsonqlParseException("Failed to parse page: " + e.getMessage(), e);
            }
        }

        return statement;
    }

    private InsertStatement parseCreate(Map<String, Object> jsonMap) throws JsonqlParseException {
        try {
            // Remove the statement field as it's already processed
            jsonMap.remove("statement");

            // Convert the Map to JSON and then to CreateStatement
            String json = objectMapper.writeValueAsString(jsonMap);
            InsertStatement statement = objectMapper.readValue(json, InsertStatement.class);

            // Set the statement type
            statement.setStatement(StatementType.INSERT);

            return statement;
        } catch (Exception e) {
            if (e instanceof JsonqlParseException) {
                throw (JsonqlParseException) e;
            }
            throw new JsonqlParseException("Failed to parse CREATE statement: " + e.getMessage(), e);
        }
    }

    private UpdateStatement parseUpdate(Map<String, Object> jsonMap) throws JsonqlParseException {
        try {
            // Remove the statement field as it's already processed
            jsonMap.remove("statement");

            // Convert the Map to JSON and then to UpdateStatement
            String json = objectMapper.writeValueAsString(jsonMap);
            UpdateStatement statement = objectMapper.readValue(json, UpdateStatement.class);

            // Set the statement type
            statement.setStatement(StatementType.UPDATE);

            return statement;
        } catch (Exception e) {
            if (e instanceof JsonqlParseException) {
                throw (JsonqlParseException) e;
            }
            throw new JsonqlParseException("Failed to parse UPDATE statement: " + e.getMessage(), e);
        }
    }

    private DeleteStatement parseDelete(Map<String, Object> jsonMap) throws JsonqlParseException {
        try {
            // Remove the statement field as it's already processed
            jsonMap.remove("statement");

            // Convert the Map to JSON and then to DeleteStatement
            String json = objectMapper.writeValueAsString(jsonMap);
            DeleteStatement statement = objectMapper.readValue(json, DeleteStatement.class);

            // Set the statement type
            statement.setStatement(StatementType.DELETE);

            return statement;
        } catch (Exception e) {
            if (e instanceof JsonqlParseException) {
                throw (JsonqlParseException) e;
            }
            throw new JsonqlParseException("Failed to parse DELETE statement: " + e.getMessage(), e);
        }
    }
} 