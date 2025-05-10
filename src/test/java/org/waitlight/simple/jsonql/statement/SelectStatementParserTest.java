package org.waitlight.simple.jsonql.statement;

import org.junit.jupiter.api.Test;
import org.waitlight.simple.jsonql.statement.model.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SelectStatementParserTest {
    private final StatementParser parser = new StatementParser();

    @Test
    public void query() throws JsonqlParseException {
        String json = """
                {
                  "statement": "query",
                  "appId": "123456",
                  "formId": "89757",
                  "entityId": "89757",
                  "filters": {
                    "rel": "or",
                    "conditions": [
                      {"field": "status", "method": "eq", "value": "active"},
                      {"field": "name", "method": "in", "values": ["A", "B"]}
                    ]
                  },
                  "sort": [
                    {"field": "name", "direction": "DESC"},
                    {"field": "createTime", "direction": "ASC"}
                  ],
                  "page": {"size": 20, "number": 1}
                }
                """;

        SelectStatement statement = (SelectStatement) parser.parse2Stmt(json);

        // 验证基本字段
        assertEquals(StatementType.SELECT, statement.getStatement());
        assertEquals("123456", statement.getAppId());
        assertEquals("89757", statement.getFormId());
        assertEquals("89757", statement.getEntityId());

        // 验证filters
        Filter filters = statement.getFilters();
        assertNotNull(filters);
        assertEquals("or", filters.getRel());
        assertEquals(2, filters.getConditions().size());

        // 验证第一个条件
        Condition firstCondition = filters.getConditions().get(0);
        assertEquals("status", firstCondition.getField());
        assertEquals(MethodType.EQ, firstCondition.getMethod());
        assertEquals("active", firstCondition.getValue());

        // 验证第二个条件
        Condition secondCondition = filters.getConditions().get(1);
        assertEquals("name", secondCondition.getField());
        assertEquals(MethodType.IN, secondCondition.getMethod());
        assertNotNull(secondCondition.getValues());
        assertEquals(2, secondCondition.getValues().size());
        assertEquals("A", secondCondition.getValues().get(0));
        assertEquals("B", secondCondition.getValues().get(1));

        // 验证排序
        List<Sort> sort = statement.getSort();
        assertNotNull(sort);
        assertEquals(2, sort.size());
        assertEquals("name", sort.get(0).getField());
        assertEquals("DESC", sort.get(0).getDirection());
        assertEquals("createTime", sort.get(1).getField());
        assertEquals("ASC", sort.get(1).getDirection());

        // 验证分页
        Page page = statement.getPage();
        assertNotNull(page);
        assertEquals(20, page.getSize());
        assertEquals(1, page.getNumber());
    }

} 