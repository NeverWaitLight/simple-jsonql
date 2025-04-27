package org.waitlight.simple.jsonql.statement.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.waitlight.simple.jsonql.statement.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JsonQLParser {
    private final ObjectMapper objectMapper;

    public JsonQLParser() {
        this.objectMapper = new ObjectMapper();
    }

    public JsonqlStatement parse(String json) throws JsonqlParseException {
        try {
            Map<String, Object> jsonMap = objectMapper.readValue(json, Map.class);
            String statementStr = (String) jsonMap.get("statement");
            if (statementStr == null) {
                throw new JsonqlParseException("Statement type is required");
            }

            StatementType statement;
            try {
                statement = StatementType.fromValue(statementStr);
            } catch (IllegalArgumentException e) {
                throw new JsonqlParseException("Unsupported statement type: " + statementStr);
            }

            switch (statement) {
                case SELECT:
                    return parseSelect(jsonMap);
                case INSERT:
                    return parseInsert(jsonMap);
                case UPDATE:
                    return parseUpdate(jsonMap);
                case DELETE:
                    return parseDelete(jsonMap);
                default:
                    throw new JsonqlParseException("Unsupported statement type: " + statement);
            }
        } catch (Exception e) {
            if (e instanceof JsonqlParseException) {
                throw (JsonqlParseException) e;
            }
            throw new JsonqlParseException("Failed to parse JSONQL: " + e.getMessage(), e);
        }
    }

    private SelectStatement parseSelect(Map<String, Object> jsonMap) throws JsonqlParseException {
        SelectStatement statement = new SelectStatement();
        statement.setStatement(StatementType.SELECT);
        statement.setSelect((List<String>) jsonMap.get("select"));
        statement.setFrom((String) jsonMap.get("from"));

        if (jsonMap.containsKey("join")) {
            Object joinObj = jsonMap.get("join");
            if (joinObj instanceof List) {
                List<Map<String, Object>> joinList = (List<Map<String, Object>>) joinObj;
                List<Join> joins = new ArrayList<>();
                for (Map<String, Object> joinMap : joinList) {
                    joins.add(parseJoin(joinMap));
                }
                statement.setJoins(joins);
            } else {
                // 单个 join 的情况
                List<Join> joins = new ArrayList<>();
                joins.add(parseJoin((Map<String, Object>) joinObj));
                statement.setJoins(joins);
            }
        }

        if (jsonMap.containsKey("where")) {
            statement.setWhere(parseWhere((Map<String, Object>) jsonMap.get("where")));
        }

        if (jsonMap.containsKey("orderBy")) {
            statement.setOrderBy(parseOrderBy((Map<String, Object>) jsonMap.get("orderBy")));
        }

        if (jsonMap.containsKey("limit")) {
            statement.setLimit((Integer) jsonMap.get("limit"));
        }

        if (jsonMap.containsKey("offset")) {
            statement.setOffset((Integer) jsonMap.get("offset"));
        }

        return statement;
    }

    private InsertStatement parseInsert(Map<String, Object> jsonMap) {
        InsertStatement statement = new InsertStatement();
        statement.setStatement(StatementType.INSERT);
        statement.setInto((String) jsonMap.get("into"));
        statement.setValues((Map<String, Object>) jsonMap.get("values"));
        return statement;
    }

    private UpdateStatement parseUpdate(Map<String, Object> jsonMap) throws JsonqlParseException {
        UpdateStatement statement = new UpdateStatement();
        statement.setStatement(StatementType.UPDATE);
        statement.setUpdate((String) jsonMap.get("update"));
        statement.setSet((Map<String, Object>) jsonMap.get("set"));

        if (jsonMap.containsKey("where")) {
            statement.setWhere(parseWhere((Map<String, Object>) jsonMap.get("where")));
        }

        return statement;
    }

    private DeleteStatement parseDelete(Map<String, Object> jsonMap) throws JsonqlParseException {
        DeleteStatement statement = new DeleteStatement();
        statement.setStatement(StatementType.DELETE);
        statement.setFrom((String) jsonMap.get("from"));

        if (jsonMap.containsKey("where")) {
            statement.setWhere(parseWhere((Map<String, Object>) jsonMap.get("where")));
        }

        return statement;
    }

    private WhereCondition parseWhere(Map<String, Object> whereMap) throws JsonqlParseException {
        String type = (String) whereMap.get("type");

        switch (type) {
            case "comparison":
                return parseComparisonCondition(whereMap);
            case "logical":
                return parseLogicalCondition(whereMap);
            case "subquery":
                return parseSubqueryCondition(whereMap);
            default:
                throw new JsonqlParseException("Unsupported where condition type: " + type);
        }
    }

    private ComparisonCondition parseComparisonCondition(Map<String, Object> conditionMap) {
        ComparisonCondition condition = new ComparisonCondition();
        condition.setField((String) conditionMap.get("field"));
        condition.setOperator((String) conditionMap.get("operator"));
        condition.setValue(conditionMap.get("value"));
        condition.setNot((Boolean) conditionMap.getOrDefault("not", false));
        return condition;
    }

    private LogicalCondition parseLogicalCondition(Map<String, Object> conditionMap) throws JsonqlParseException {
        LogicalCondition condition = new LogicalCondition();
        condition.setOperator((String) conditionMap.get("operator"));

        List<Map<String, Object>> conditions = (List<Map<String, Object>>) conditionMap.get("conditions");
        List<WhereCondition> parsedConditions = new ArrayList<>();

        for (Map<String, Object> cond : conditions) {
            parsedConditions.add(parseWhere(cond));
        }

        condition.setConditions(parsedConditions);
        return condition;
    }

    private SubqueryCondition parseSubqueryCondition(Map<String, Object> conditionMap) throws JsonqlParseException {
        SubqueryCondition condition = new SubqueryCondition();
        condition.setSubquery(parseSelect((Map<String, Object>) conditionMap.get("subquery")));
        return condition;
    }

    private OrderBy parseOrderBy(Map<String, Object> orderByMap) {
        OrderBy orderBy = new OrderBy();
        orderBy.setField((String) orderByMap.get("field"));
        orderBy.setDirection((String) orderByMap.get("direction"));
        return orderBy;
    }

    private Join parseJoin(Map<String, Object> joinMap) throws JsonqlParseException {
        Join join = new Join();
        join.setType((String) joinMap.get("type"));
        join.setTable((String) joinMap.get("table"));
        join.setOn(parseWhere((Map<String, Object>) joinMap.get("on")));
        return join;
    }
} 