package org.waitlight.simple.jsonql.statement;

import org.junit.jupiter.api.Test;
import org.waitlight.simple.jsonql.statement.model.*;
import org.waitlight.simple.jsonql.statement.parser.JsonQLParser;
import org.waitlight.simple.jsonql.statement.parser.JsonqlParseException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class JsonQLParserTest {
    private final JsonQLParser parser = new JsonQLParser();

    @Test
    public void testParseSimpleSelect() throws JsonqlParseException {
        String json = """
                {
                    "statement": "select",
                    "select": ["id", "name", "age"],
                    "from": "user",
                    "where": {
                        "type": "comparison",
                        "field": "age",
                        "operator": "gt",
                        "value": 18
                    }
                }
                """;


        SelectStatement statement = (SelectStatement) parser.parse(json);

        assertEquals(StatementType.SELECT, statement.getStatement());
        assertEquals(3, statement.getSelect().size());
        assertEquals("user", statement.getFrom());

        ComparisonCondition where = (ComparisonCondition) statement.getWhere();
        assertEquals("comparison", where.getType());
        assertEquals("age", where.getField());
        assertEquals("gt", where.getOperator());
        assertEquals(18, where.getValue());
    }

    @Test
    public void testParseInsert() throws JsonqlParseException {
        String json = """
                {
                    "statement": "insert",
                    "into": "user",
                    "values": {
                        "name": "张三",
                        "age": 25,
                        "email": "zhangsan@example.com"
                    }
                }
                """;

        InsertStatement statement = (InsertStatement) parser.parse(json);

        assertEquals(StatementType.INSERT, statement.getStatement());
        assertEquals("user", statement.getInto());
        assertEquals(3, statement.getValues().size());
        assertEquals("张三", statement.getValues().get("name"));
        assertEquals(25, statement.getValues().get("age"));
        assertEquals("zhangsan@example.com", statement.getValues().get("email"));
    }

    @Test
    public void testParseUpdate() throws JsonqlParseException {
        String json = """
                {
                    "statement": "update",
                    "update": "user",
                    "set": {
                        "age": 26,
                        "status": "inactive"
                    },
                    "where": {
                        "type": "comparison",
                        "field": "id",
                        "operator": "eq",
                        "value": 123
                    }
                }
                """;

        UpdateStatement statement = (UpdateStatement) parser.parse(json);

        assertEquals(StatementType.UPDATE, statement.getStatement());
        assertEquals("user", statement.getUpdate());
        assertEquals(2, statement.getSet().size());
        assertEquals(26, statement.getSet().get("age"));
        assertEquals("inactive", statement.getSet().get("status"));

        ComparisonCondition where = (ComparisonCondition) statement.getWhere();
        assertEquals("comparison", where.getType());
        assertEquals("id", where.getField());
        assertEquals("eq", where.getOperator());
        assertEquals(123, where.getValue());
    }

    @Test
    public void testParseDelete() throws JsonqlParseException {
        String json = """
                {
                    "statement": "delete",
                    "from": "user",
                    "where": {
                        "type": "logical",
                        "operator": "and",
                        "conditions": [
                            {
                                "type": "comparison",
                                "field": "status",
                                "operator": "eq",
                                "value": "inactive"
                            },
                            {
                                "type": "comparison",
                                "field": "created_at",
                                "operator": "lt",
                                "value": "2025-01-01"
                            }
                        ]
                    }
                }
                """;

        DeleteStatement statement = (DeleteStatement) parser.parse(json);

        assertEquals(StatementType.DELETE, statement.getStatement());
        assertEquals("user", statement.getFrom());

        LogicalCondition where = (LogicalCondition) statement.getWhere();
        assertEquals("logical", where.getType());
        assertEquals("and", where.getOperator());
        assertEquals(2, where.getConditions().size());

        ComparisonCondition firstCondition = (ComparisonCondition) where.getConditions().get(0);
        assertEquals("status", firstCondition.getField());
        assertEquals("eq", firstCondition.getOperator());
        assertEquals("inactive", firstCondition.getValue());

        ComparisonCondition secondCondition = (ComparisonCondition) where.getConditions().get(1);
        assertEquals("created_at", secondCondition.getField());
        assertEquals("lt", secondCondition.getOperator());
        assertEquals("2025-01-01", secondCondition.getValue());
    }

    @Test
    public void testParseComplexSelect() throws JsonqlParseException {
        String json = """
                {
                    "statement": "select",
                    "select": ["u.id", "u.name", "u.age", "d.department_name", "d.location"],
                    "from": "user u",
                    "join": {
                        "type": "left",
                        "table": "department d",
                        "on": {
                            "type": "comparison",
                            "field": "u.department_id",
                            "operator": "eq",
                            "value": "d.id"
                        }
                    },
                    "where": {
                        "type": "logical",
                        "operator": "and",
                        "conditions": [
                            {
                                "type": "comparison",
                                "field": "u.age",
                                "operator": "gt",
                                "value": 25
                            },
                            {
                                "type": "comparison",
                                "field": "u.status",
                                "operator": "eq",
                                "value": "active"
                            }
                        ]
                    },
                    "orderBy": {
                        "field": "u.age",
                        "direction": "desc"
                    },
                    "limit": 10,
                    "offset": 20
                }
                """;

        SelectStatement statement = (SelectStatement) parser.parse(json);

        // 验证基本字段
        assertEquals(StatementType.SELECT, statement.getStatement());
        assertEquals(5, statement.getSelect().size());
        assertEquals("user u", statement.getFrom());
        assertEquals(10, statement.getLimit());
        assertEquals(20, statement.getOffset());

        // 验证 JOIN
        List<Join> joins = statement.getJoins();
        assertEquals(1, joins.size());
        Join join = joins.get(0);
        assertEquals("left", join.getType());
        assertEquals("department d", join.getTable());

        ComparisonCondition joinCondition = (ComparisonCondition) join.getOn();
        assertEquals("comparison", joinCondition.getType());
        assertEquals("u.department_id", joinCondition.getField());
        assertEquals("eq", joinCondition.getOperator());
        assertEquals("d.id", joinCondition.getValue());

        // 验证排序
        OrderBy orderBy = statement.getOrderBy();
        assertEquals("u.age", orderBy.getField());
        assertEquals("desc", orderBy.getDirection());

        // 验证 WHERE 条件
        LogicalCondition where = (LogicalCondition) statement.getWhere();
        assertEquals("logical", where.getType());
        assertEquals("and", where.getOperator());
        assertEquals(2, where.getConditions().size());

        // 验证第一个条件
        ComparisonCondition firstCondition = (ComparisonCondition) where.getConditions().get(0);
        assertEquals("comparison", firstCondition.getType());
        assertEquals("u.age", firstCondition.getField());
        assertEquals("gt", firstCondition.getOperator());
        assertEquals(25, firstCondition.getValue());

        // 验证第二个条件
        ComparisonCondition secondCondition = (ComparisonCondition) where.getConditions().get(1);
        assertEquals("comparison", secondCondition.getType());
        assertEquals("u.status", secondCondition.getField());
        assertEquals("eq", secondCondition.getOperator());
        assertEquals("active", secondCondition.getValue());
    }

    @Test
    public void testParseMultipleJoins() throws JsonqlParseException {
        String json = """
                {
                    "statement": "select",
                    "select": ["u.id", "u.name", "d.department_name", "r.role_name", "p.project_name"],
                    "from": "user u",
                    "join": [
                        {
                            "type": "left",
                            "table": "department d",
                            "on": {
                                "type": "comparison",
                                "field": "u.department_id",
                                "operator": "eq",
                                "value": "d.id"
                            }
                        },
                        {
                            "type": "inner",
                            "table": "role r",
                            "on": {
                                "type": "comparison",
                                "field": "u.role_id",
                                "operator": "eq",
                                "value": "r.id"
                            }
                        },
                        {
                            "type": "right",
                            "table": "project p",
                            "on": {
                                "type": "comparison",
                                "field": "u.project_id",
                                "operator": "eq",
                                "value": "p.id"
                            }
                        }
                    ],
                    "where": {
                        "type": "comparison",
                        "field": "u.status",
                        "operator": "eq",
                        "value": "active"
                    }
                }
                """;

        SelectStatement statement = (SelectStatement) parser.parse(json);

        // 验证基本字段
        assertEquals(StatementType.SELECT, statement.getStatement());
        assertEquals(5, statement.getSelect().size());
        assertEquals("user u", statement.getFrom());

        // 验证 JOIN 列表
        List<Join> joins = statement.getJoins();
        assertEquals(3, joins.size());

        // 验证 LEFT JOIN
        Join leftJoin = joins.get(0);
        assertEquals("left", leftJoin.getType());
        assertEquals("department d", leftJoin.getTable());
        ComparisonCondition leftJoinCondition = (ComparisonCondition) leftJoin.getOn();
        assertEquals("u.department_id", leftJoinCondition.getField());
        assertEquals("eq", leftJoinCondition.getOperator());
        assertEquals("d.id", leftJoinCondition.getValue());

        // 验证 INNER JOIN
        Join innerJoin = joins.get(1);
        assertEquals("inner", innerJoin.getType());
        assertEquals("role r", innerJoin.getTable());
        ComparisonCondition innerJoinCondition = (ComparisonCondition) innerJoin.getOn();
        assertEquals("u.role_id", innerJoinCondition.getField());
        assertEquals("eq", innerJoinCondition.getOperator());
        assertEquals("r.id", innerJoinCondition.getValue());

        // 验证 RIGHT JOIN
        Join rightJoin = joins.get(2);
        assertEquals("right", rightJoin.getType());
        assertEquals("project p", rightJoin.getTable());
        ComparisonCondition rightJoinCondition = (ComparisonCondition) rightJoin.getOn();
        assertEquals("u.project_id", rightJoinCondition.getField());
        assertEquals("eq", rightJoinCondition.getOperator());
        assertEquals("p.id", rightJoinCondition.getValue());

        // 验证 WHERE 条件
        ComparisonCondition where = (ComparisonCondition) statement.getWhere();
        assertEquals("comparison", where.getType());
        assertEquals("u.status", where.getField());
        assertEquals("eq", where.getOperator());
        assertEquals("active", where.getValue());
    }

    @Test
    public void testParseComplexJoinsWithConditions() throws JsonqlParseException {
        String json = """
                {
                    "statement": "select",
                    "select": ["e.id", "e.name", "d.department_name", "m.name as manager_name"],
                    "from": "employee e",
                    "join": [
                        {
                            "type": "left",
                            "table": "department d",
                            "on": {
                                "type": "logical",
                                "operator": "and",
                                "conditions": [
                                    {
                                        "type": "comparison",
                                        "field": "e.department_id",
                                        "operator": "eq",
                                        "value": "d.id"
                                    },
                                    {
                                        "type": "comparison",
                                        "field": "d.status",
                                        "operator": "eq",
                                        "value": "active"
                                    }
                                ]
                            }
                        },
                        {
                            "type": "left",
                            "table": "employee m",
                            "on": {
                                "type": "logical",
                                "operator": "and",
                                "conditions": [
                                    {
                                        "type": "comparison",
                                        "field": "e.manager_id",
                                        "operator": "eq",
                                        "value": "m.id"
                                    },
                                    {
                                        "type": "comparison",
                                        "field": "m.status",
                                        "operator": "eq",
                                        "value": "active"
                                    }
                                ]
                            }
                        }
                    ],
                    "where": {
                        "type": "comparison",
                        "field": "e.status",
                        "operator": "eq",
                        "value": "active"
                    }
                }
                """;

        SelectStatement statement = (SelectStatement) parser.parse(json);

        // 验证基本字段
        assertEquals(StatementType.SELECT, statement.getStatement());
        assertEquals(4, statement.getSelect().size());
        assertEquals("employee e", statement.getFrom());

        // 验证 JOIN 列表
        List<Join> joins = statement.getJoins();
        assertEquals(2, joins.size());

        // 验证第一个 LEFT JOIN (部门)
        Join deptJoin = joins.get(0);
        assertEquals("left", deptJoin.getType());
        assertEquals("department d", deptJoin.getTable());
        LogicalCondition deptJoinCondition = (LogicalCondition) deptJoin.getOn();
        assertEquals("and", deptJoinCondition.getOperator());
        assertEquals(2, deptJoinCondition.getConditions().size());

        // 验证第二个 LEFT JOIN (经理)
        Join managerJoin = joins.get(1);
        assertEquals("left", managerJoin.getType());
        assertEquals("employee m", managerJoin.getTable());
        LogicalCondition managerJoinCondition = (LogicalCondition) managerJoin.getOn();
        assertEquals("and", managerJoinCondition.getOperator());
        assertEquals(2, managerJoinCondition.getConditions().size());

        // 验证 WHERE 条件
        ComparisonCondition where = (ComparisonCondition) statement.getWhere();
        assertEquals("comparison", where.getType());
        assertEquals("e.status", where.getField());
        assertEquals("eq", where.getOperator());
        assertEquals("active", where.getValue());
    }

    @Test
    public void testParseInvalidJson() {
        String invalidJson = "{ invalid json }";

        assertThrows(JsonqlParseException.class, () -> {
            parser.parse(invalidJson);
        });
    }

    @Test
    public void testParseUnsupportedStatement() {
        String json = """
                {
                    "statement": "unsupported",
                    "select": ["id"]
                }
                """;

        assertThrows(JsonqlParseException.class, () -> {
            parser.parse(json);
        });
    }

    @Test
    public void testParseInvalidStatementType() {
        String json = """
                {
                    "statement": "invalid",
                    "select": ["id"]
                }
                """;

        JsonqlParseException exception = assertThrows(JsonqlParseException.class, () -> {
            parser.parse(json);
        });
        assertEquals("Unsupported statement type: invalid", exception.getMessage());
    }

    @Test
    public void testParseMissingStatementType() {
        String json = """
                {
                    "select": ["id"]
                }
                """;

        JsonqlParseException exception = assertThrows(JsonqlParseException.class, () -> {
            parser.parse(json);
        });
        assertEquals("Statement type is required", exception.getMessage());
    }

    @Test
    public void testParseSelectWithBetweenCondition() throws JsonqlParseException {
        String json = """
                {
                    "statement": "select",
                    "select": ["id", "name", "age", "salary"],
                    "from": "employee",
                    "where": {
                        "type": "between",
                        "field": "salary",
                        "start": 5000,
                        "end": 10000
                    }
                }
                """;

        SelectStatement statement = (SelectStatement) parser.parse(json);

        // 验证基本字段
        assertEquals(StatementType.SELECT, statement.getStatement());
        assertEquals(4, statement.getSelect().size());
        assertEquals("employee", statement.getFrom());

        // 验证 BETWEEN 条件
        BetweenCondition where = (BetweenCondition) statement.getWhere();
        assertEquals("between", where.getType());
        assertEquals("salary", where.getField());
        assertEquals(5000, where.getStart());
        assertEquals(10000, where.getEnd());
    }
} 