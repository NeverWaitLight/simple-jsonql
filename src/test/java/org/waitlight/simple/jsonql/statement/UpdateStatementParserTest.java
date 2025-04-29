package org.waitlight.simple.jsonql.statement;

import org.junit.jupiter.api.Test;
import org.waitlight.simple.jsonql.statement.model.Field;
import org.waitlight.simple.jsonql.statement.model.NestedEntity;
import org.waitlight.simple.jsonql.statement.model.StatementType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class UpdateStatementParserTest {
    private final JsonQLParser parser = new JsonQLParser();

    @Test
    public void update() throws JsonqlParseException {
        String json = """
                {
                    "appId": "123456",
                    "formId": "89757",
                    "entityId": "147258",
                    "dataId": "1",
                    "statement": "update",
                    "fields": [
                        {"field": "name", "value": "高桥凉介"},
                        {
                            "field": "blogs",
                            "values": [
                                {
                                    "appId": "123456",
                                    "formId": "89758",
                                    "entityId": "147259",
                                    "dataId": "321",
                                    "fields": [ {"field": "title", "value": "生死疲劳"} ]
                                }
                            ]
                        }
                    ]
                }
                """;

        UpdateStatement statement = (UpdateStatement) parser.parse(json);

        assertEquals(StatementType.UPDATE, statement.getStatement());
        assertEquals("123456", statement.getAppId());
        assertEquals("89757", statement.getFormId());
        assertEquals("147258", statement.getEntityId());
        assertEquals("1", statement.getDataId());
        
        assertNotNull(statement.getFields());
        assertEquals(2, statement.getFields().size());
        
        // 验证第一个字段
        Field nameField = statement.getFields().get(0);
        assertEquals("name", nameField.getField());
        assertEquals("高桥凉介", nameField.getValue());
        
        // 验证第二个字段（嵌套实体）
        Field blogsField = statement.getFields().get(1);
        assertEquals("blogs", blogsField.getField());
        assertNotNull(blogsField.getValues());
        assertEquals(1, blogsField.getValues().size());
        
        // 验证嵌套实体
        NestedEntity blog = blogsField.getValues().get(0);
        assertEquals("123456", blog.getAppId());
        assertEquals("89758", blog.getFormId());
        assertEquals("147259", blog.getEntityId());
        assertEquals("321", blog.getDataId());
        assertNotNull(blog.getFields());
        assertEquals(1, blog.getFields().size());
        
        // 验证嵌套实体的字段
        Field titleField = blog.getFields().get(0);
        assertEquals("title", titleField.getField());
        assertEquals("生死疲劳", titleField.getValue());
    }
} 