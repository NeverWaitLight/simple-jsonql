package org.waitlight.simple.jsonql.engine;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.waitlight.simple.jsonql.entity.Blog;
import org.waitlight.simple.jsonql.entity.User;
import org.waitlight.simple.jsonql.metadata.MetadataSources;

import static org.junit.jupiter.api.Assertions.*;

public class InsertEngineTest {
    private static JsonQLEngine engine;

    @BeforeAll
    public static void setUp() {
        MetadataSources metadataSources = new MetadataSources();
        metadataSources.addAnnotatedClass(User.class);
        metadataSources.addAnnotatedClass(Blog.class);
        engine = new JsonQLEngine(metadataSources);
    }

    @Test
    public void testCreateSingleEntity() throws Exception {
        // Create a single user entity
        String jsonCreate = """
                {
                    "statement": "create",
                    "appId": "123456",
                    "formId": "89757",
                    "entityId": "user",
                    "fields": [
                        {"field": "name", "value": "tom"}
                    ]
                }
                """;

        Object result = engine.execute(jsonCreate);

        // The result should be the number of affected rows
        assertNotNull(result);
        assertTrue(result instanceof Integer);
        assertEquals(1, result);
    }

    @Test
    public void testCreateWithNestedEntities() throws Exception {
        // Create a user with nested blog entities
        String jsonCreate = """
                {
                    "statement": "create",
                    "appId": "123456",
                    "formId": "89757",
                    "entityId": "user",
                    "fields": [
                        {"field": "name", "value": "jerry"},
                        {
                            "field": "blogs",
                            "values": [
                                {
                                    "appId": "123456",
                                    "formId": "89758",
                                    "entityId": "blog",
                                    "fields": [
                                        {"field": "title", "value": "太阳照常升起"},
                                        {"field": "content", "value": "这是一篇博客内容"}
                                    ]
                                },
                                {
                                    "appId": "123456",
                                    "formId": "89758",
                                    "entityId": "blog",
                                    "fields": [
                                        {"field": "title", "value": "活着"},
                                        {"field": "content", "value": "这是第二篇博客内容"}
                                    ]
                                }
                            ]
                        }
                    ]
                }
                """;

        Object result = engine.execute(jsonCreate);

        assertNotNull(result);
        assertTrue(result instanceof Integer);
        assertEquals(3, result);
    }

    @Test
    public void testCreateWithInvalidField() throws Exception {
        // Create with a field that doesn't exist in the entity
        String jsonCreate = """
                {
                    "statement": "create",
                    "appId": "123456",
                    "formId": "89757",
                    "entityId": "user",
                    "fields": [
                        {"field": "id", "value": 3},
                        {"field": "name", "value": "alice"},
                        {"field": "nonexistent", "value": "this field doesn't exist"}
                    ]
                }
                """;

        Exception exception = assertThrows(Exception.class, () -> engine.execute(jsonCreate));
        assertTrue(exception.getMessage().contains("nonexistent") || exception.getMessage().contains("field"));
    }

    @Test
    public void testCreateBlogDirectly() throws Exception {
        String jsonCreate = """
                {
                  "statement": "create",
                  "appId": "123456",
                  "formId": "89758",
                  "entityId": "blog",
                  "fields": [
                    {"field": "title", "value": "用户123的新博客"},
                    {"field": "content", "value": "这是用户123的新博客内容"},
                    {
                      "field": "user",
                      "values": [
                        {
                          "appId": "123456",
                          "formId": "89758",
                          "entityId": "user",
                          "fields": [ {"field": "id", "value": "123"} ]
                        }
                      ]
                    }
                  ]
                }
                """;

        Object result = engine.execute(jsonCreate);

        assertNotNull(result);
        assertTrue(result instanceof Integer);
        assertEquals(1, result); // 应该插入1条记录
    }
}