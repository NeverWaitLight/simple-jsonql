package org.waitlight.simple.jsonql.statement;

import org.junit.jupiter.api.Test;
import org.waitlight.simple.jsonql.statement.model.FieldStatement;
import org.waitlight.simple.jsonql.statement.model.NestedStatement;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class InsertStatementParserTest {
    private final StatementParser parser = new StatementParser();

    @Test
    public void create() throws JsonQLStatementException {
        String json = """
                {
                    "appId": "123456",
                    "formId": "89757",
                    "entityId": "user",
                    "fields": [
                        {"field": "name", "value": "张三"},
                        {"field": "age", "value": 25},
                        {"field": "email", "value": "zhangsan@example.com"}
                    ]
                }
                """;

        InsertStatement statement = parser.parse(json, InsertStatement.class);

        assertEquals("user", statement.getEntityId());
        assertEquals("123456", statement.getAppId());
        assertEquals("89757", statement.getFormId());
        assertEquals(3, statement.getFields().size());

        assertEquals("name", statement.getFields().get(0).getField());
        assertEquals("张三", statement.getFields().get(0).getValue());

        assertEquals("age", statement.getFields().get(1).getField());
        assertEquals(25, statement.getFields().get(1).getValue());

        assertEquals("email", statement.getFields().get(2).getField());
        assertEquals("zhangsan@example.com", statement.getFields().get(2).getValue());
    }

    @Test
    public void createWithNestedEntities() throws JsonQLStatementException {
        String json = """
                {
                    "appId": "123456",
                    "formId": "89757",
                    "entityId": "user",
                    "fields": [
                        {"field": "name", "value": "tom"},
                        {
                            "field": "blogs",
                            "values": [
                                {
                                    "appId": "123456",
                                    "formId": "89758",
                                    "entityId": "147259",
                                    "fields": [ {"field": "title", "value": "太阳照常升起"} ]
                                },
                                {
                                    "appId": "123456",
                                    "formId": "89758",
                                    "entityId": "147259",
                                    "fields": [ {"field": "title", "value": "活着"} ]
                                }
                            ]
                        }
                    ]
                }
                """;

        InsertStatement statement = parser.parse(json, InsertStatement.class);

        assertEquals("user", statement.getEntityId());
        assertEquals("123456", statement.getAppId());
        assertEquals("89757", statement.getFormId());
        assertEquals(2, statement.getFields().size());

        // Verify first field
        FieldStatement nameField = statement.getFields().get(0);
        assertEquals("name", nameField.getField());
        assertEquals("tom", nameField.getValue());

        // Verify nested entity field
        FieldStatement blogsField = statement.getFields().get(1);
        assertEquals("blogs", blogsField.getField());
        assertEquals(2, blogsField.getValues().size());

        // Verify first nested entity
        NestedStatement firstBlog = blogsField.getValues().get(0);
        assertEquals("123456", firstBlog.getAppId());
        assertEquals("89758", firstBlog.getFormId());
        assertEquals("147259", firstBlog.getEntityId());
        assertEquals(1, firstBlog.getFields().size());
        assertEquals("title", firstBlog.getFields().get(0).getField());
        assertEquals("太阳照常升起", firstBlog.getFields().get(0).getValue());

        // Verify second nested entity
        NestedStatement secondBlog = blogsField.getValues().get(1);
        assertEquals("123456", secondBlog.getAppId());
        assertEquals("89758", secondBlog.getFormId());
        assertEquals("147259", secondBlog.getEntityId());
        assertEquals(1, secondBlog.getFields().size());
        assertEquals("title", secondBlog.getFields().get(0).getField());
        assertEquals("活着", secondBlog.getFields().get(0).getValue());
    }
} 