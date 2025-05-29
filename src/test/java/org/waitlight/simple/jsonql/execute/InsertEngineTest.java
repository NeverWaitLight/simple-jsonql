package org.waitlight.simple.jsonql.execute;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.waitlight.simple.jsonql.entity.Blog;
import org.waitlight.simple.jsonql.entity.User;
import org.waitlight.simple.jsonql.execute.result.ExecuteResult;
import org.waitlight.simple.jsonql.execute.result.InsertResult;
import org.waitlight.simple.jsonql.metadata.MetadataSource;
import org.waitlight.simple.jsonql.statement.InsertStatement;

import static org.junit.jupiter.api.Assertions.*;

public class InsertEngineTest {
    private static ExecuteEngine engine;

    @BeforeAll
    public static void setUp() {
        MetadataSource metadataSource = new MetadataSource();
        metadataSource.registry(User.class);
        metadataSource.registry(Blog.class);
        engine = new ExecuteEngine(metadataSource);
    }

    @Test
    public void execute_singleEntityInsert_returnsSuccessResult() throws Exception {
        // Create a single user entity
        String jsonCreate = """
                {
                    "appId": "123456",
                    "formId": "89757",
                    "entityId": "user",
                    "fields": [
                        {"field": "name", "value": "tom"}
                    ]
                }
                """;

        ExecuteResult result = engine.execute(jsonCreate, InsertStatement.class);

        // The result should be an InsertResult
        assertNotNull(result);
        assertInstanceOf(InsertResult.class, result);
        InsertResult insertResult = (InsertResult) result;
        assertTrue(insertResult.getAffectedRows() > 0);
        assertFalse(insertResult.getMainIds().isEmpty());
    }

    @Test
    public void execute_insertWithNestedEntities_returnsSuccessResult() throws Exception {
        // Create a user with nested blog entities
        String jsonCreate = """
                {
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

        ExecuteResult result = engine.execute(jsonCreate, InsertStatement.class);

        assertNotNull(result);
        assertInstanceOf(InsertResult.class, result);
        InsertResult insertResult = (InsertResult) result;
        assertTrue(insertResult.getAffectedRows() > 0);
        assertFalse(insertResult.getMainIds().isEmpty());
        assertFalse(insertResult.getNestedIds().isEmpty());
    }

    @Test
    public void execute_insertWithInvalidField_throwsException() throws Exception {
        String jsonCreate = """
                {
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

        Exception exception = assertThrows(Exception.class, () -> engine.execute(jsonCreate, InsertStatement.class));
        assertTrue(exception.getMessage().contains("nonexistent") || exception.getMessage().contains("field"));
    }

    @Test
    public void execute_createBlogDirectly_returnsSuccessResult() throws Exception {
        String jsonCreate = """
                {
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

        ExecuteResult result = engine.execute(jsonCreate, InsertStatement.class);

        assertNotNull(result);
        assertTrue(result instanceof InsertResult);
        InsertResult insertResult = (InsertResult) result;
        assertTrue(insertResult.getAffectedRows() > 0);
        assertFalse(insertResult.getMainIds().isEmpty());
    }

    @Test
    public void execute_createBlogWithNewUser_returnsSuccessResult() throws Exception {
        String jsonCreate = """
                {
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
                          "fields": [ {"field": "name", "value": "无敌旋风腿"} ]
                        }
                      ]
                    }
                  ]
                }
                """;

        ExecuteResult result = engine.execute(jsonCreate, InsertStatement.class);

        assertNotNull(result);
        assertTrue(result instanceof InsertResult);
        InsertResult insertResult = (InsertResult) result;
        assertTrue(insertResult.getAffectedRows() > 0);
        assertFalse(insertResult.getMainIds().isEmpty());
    }
}