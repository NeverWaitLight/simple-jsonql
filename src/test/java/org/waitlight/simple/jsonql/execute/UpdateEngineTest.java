package org.waitlight.simple.jsonql.execute;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.waitlight.simple.jsonql.entity.Blog;
import org.waitlight.simple.jsonql.entity.User;
import org.waitlight.simple.jsonql.execute.result.ExecuteResult;
import org.waitlight.simple.jsonql.execute.result.InsertResult;
import org.waitlight.simple.jsonql.execute.result.UpdateResult;
import org.waitlight.simple.jsonql.metadata.MetadataSource;
import org.waitlight.simple.jsonql.statement.InsertStatement;
import org.waitlight.simple.jsonql.statement.UpdateStatement;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class UpdateEngineTest {
    private static ExecuteEngine engine;

    @BeforeAll
    public static void setUp() {
        MetadataSource metadataSource = new MetadataSource();
        metadataSource.registry(User.class);
        metadataSource.registry(Blog.class);
        engine = new ExecuteEngine(metadataSource);
    }

    @Test
    public void execute_singleEntityUpdate_returnsSuccessResult() throws Exception {
        // Create a user first to update
        String originalName = "原始名称" + new Random().nextInt(10000);
        String jsonCreate = """
                {
                          "appId": "123456",
                          "formId": "89757",
                          "entityId": "user",
                          "fields": [
                              {"field": "name", "value": "%s"}
                          ]
                      }
                """.formatted(originalName);

        ExecuteResult createResult = engine.execute(jsonCreate, InsertStatement.class);
        assertNotNull(createResult);
        assertInstanceOf(InsertResult.class, createResult);
        InsertResult insertResult = (InsertResult) createResult;
        assertTrue(insertResult.getAffectedRows() > 0);
        assertFalse(insertResult.getMainIds().isEmpty());

        Long userId = insertResult.getMainIds().getFirst();

        // Update the user
        String updatedName = "更新后名称" + new Random().nextInt(10000);
        String jsonUpdate = """
                {
                        "appId": "123456",
                        "formId": "89757",
                        "entityId": "user",
                        "dataId": "%s",
                        "fields": [
                            {"field": "name", "value": "%s"}
                        ]
                    }
                """.formatted(userId, updatedName);

        ExecuteResult result = engine.execute(jsonUpdate, UpdateStatement.class);

        // The result should be an UpdateResult
        assertNotNull(result);
        assertInstanceOf(UpdateResult.class, result);
        UpdateResult updateResult = (UpdateResult) result;
        assertTrue(updateResult.getAffectedRows() > 0);
    }

    @Test
    public void execute_updateWithNestedEntities_returnsSuccessResult() throws Exception {
        // Create a user with nested blog entities first
        String userName = "用户" + new Random().nextInt(10000);
        String blogTitle = "博客标题" + new Random().nextInt(10000);
        String jsonCreate = """
                {
                    "appId": "123456",
                    "formId": "89757",
                    "entityId": "user",
                    "fields": [
                        {"field": "name", "value": "%s"},
                        {
                            "field": "blogs",
                            "values": [
                                {
                                    "appId": "123456",
                                    "formId": "89758",
                                    "entityId": "blog",
                                    "fields": [
                                        {"field": "title", "value": "%s"},
                                        {"field": "content", "value": "这是博客内容"}
                                    ]
                                }
                            ]
                        }
                    ]
                }
                """.formatted(userName, blogTitle);

        ExecuteResult createResult = engine.execute(jsonCreate, InsertStatement.class);
        assertNotNull(createResult);
        assertInstanceOf(InsertResult.class, createResult);
        InsertResult insertResult = (InsertResult) createResult;
        assertTrue(insertResult.getAffectedRows() > 0);
        assertFalse(insertResult.getMainIds().isEmpty());
        assertFalse(insertResult.getNestedIds().isEmpty());

        Long userId = insertResult.getMainIds().getFirst();
        Long blogId = insertResult.getNestedIds().getFirst();

        // Update the user with nested blog reference
        String updatedName = "更新后用户名" + new Random().nextInt(10000);
        String jsonUpdate = """
                {
                        "appId": "123456",
                        "formId": "89757",
                        "entityId": "user",
                        "dataId": "%s",
                        "fields": [
                            {"field": "name", "value": "%s"},
                            {
                                "field": "blogs",
                                "values": [
                                    {
                                        "appId": "123456",
                                        "formId": "89758",
                                        "entityId": "blog",
                                        "dataId": "%s",
                                        "fields": [
                                            {"field": "title", "value": "更新后的博客标题"}
                                        ]
                                }
                            ]
                        }
                    ]
                }
                """.formatted(userId, updatedName, blogId);

        ExecuteResult result = engine.execute(jsonUpdate, UpdateStatement.class);

        assertNotNull(result);
        assertInstanceOf(UpdateResult.class, result);
        UpdateResult updateResult = (UpdateResult) result;
        assertTrue(updateResult.getAffectedRows() > 0);
    }

    @Test
    public void execute_updateWithInvalidField_throwsException() {
        // Create a user first
        String originalName = "测试用户" + new Random().nextInt(10000);
        String jsonCreate = """
                {
                    "appId": "123456",
                    "formId": "89757",
                    "entityId": "user",
                    "fields": [
                        {"field": "name", "value": "%s"}
                    ]
                }
                """.formatted(originalName);

        ExecuteResult createResult = assertDoesNotThrow(() -> engine.execute(jsonCreate, InsertStatement.class));
        InsertResult insertResult = (InsertResult) createResult;
        Long userId = insertResult.getMainIds().get(0);

        // Try to update with invalid field
        String jsonUpdate = """
                {
                    "appId": "123456",
                    "formId": "89757",
                    "entityId": "user",
                    "dataId": "%s",
                    "fields": [
                        {"field": "name", "value": "alice"},
                        {"field": "nonexistent", "value": "this field doesn't exist"}
                    ]
                }
                """.formatted(userId);

        Exception exception = assertThrows(Exception.class, () -> engine.execute(jsonUpdate, UpdateStatement.class));
        assertTrue(exception.getMessage().contains("nonexistent") || exception.getMessage().contains("field"));
    }

    @Test
    public void execute_updateBlogDirectly_returnsSuccessResult() throws Exception {
        // Create a blog with user reference first
        String jsonCreate = """
                {
                  "appId": "123456",
                  "formId": "89758",
                  "entityId": "blog",
                  "fields": [
                    {"field": "title", "value": "原始博客标题"},
                    {"field": "content", "value": "这是原始博客内容"},
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

        ExecuteResult createResult = engine.execute(jsonCreate, InsertStatement.class);
        assertNotNull(createResult);
        assertInstanceOf(InsertResult.class, createResult);
        InsertResult insertResult = (InsertResult) createResult;
        assertTrue(insertResult.getAffectedRows() > 0);
        assertFalse(insertResult.getMainIds().isEmpty());

        Long blogId = insertResult.getMainIds().get(0);

        // Update the blog
        String jsonUpdate = """
                {
                  "appId": "123456",
                  "formId": "89758",
                  "entityId": "blog",
                  "dataId": "%s",
                  "fields": [
                    {"field": "title", "value": "更新后的博客标题"},
                    {"field": "content", "value": "这是更新后的博客内容"}
                  ]
                }
                """.formatted(blogId);

        ExecuteResult result = engine.execute(jsonUpdate, UpdateStatement.class);

        assertNotNull(result);
        assertInstanceOf(UpdateResult.class, result);
        UpdateResult updateResult = (UpdateResult) result;
        assertTrue(updateResult.getAffectedRows() > 0);
    }

    @Test
    public void execute_updateBlogWithUserReference_returnsSuccessResult() throws Exception {
        // Create a blog first
        String jsonCreate = """
                {
                  "appId": "123456",
                  "formId": "89758",
                  "entityId": "blog",
                  "fields": [
                    {"field": "title", "value": "原始博客"},
                    {"field": "content", "value": "原始内容"},
                    {
                      "field": "user",
                      "values": [
                        {
                          "appId": "123456",
                          "formId": "89758",
                          "entityId": "user",
                          "fields": [ {"field": "name", "value": "原始作者"} ]
                        }
                      ]
                    }
                  ]
                }
                """;

        ExecuteResult createResult = engine.execute(jsonCreate, InsertStatement.class);
        assertNotNull(createResult);
        assertInstanceOf(InsertResult.class, createResult);
        InsertResult insertResult = (InsertResult) createResult;
        assertTrue(insertResult.getAffectedRows() > 0);
        assertFalse(insertResult.getMainIds().isEmpty());

        Long blogId = insertResult.getMainIds().get(0);

        // Update the blog with user reference
        String jsonUpdate = """
                {
                  "appId": "123456",
                  "formId": "89758",
                  "entityId": "blog",
                  "dataId": "%s",
                  "fields": [
                    {"field": "title", "value": "更新后博客标题"},
                    {"field": "content", "value": "更新后博客内容"},
                    {
                      "field": "user",
                      "values": [
                        {
                          "appId": "123456",
                          "formId": "89758",
                          "entityId": "user",
                          "fields": [ {"field": "id", "value": "456"} ]
                        }
                      ]
                    }
                  ]
                    }
                """.formatted(blogId);

        ExecuteResult result = engine.execute(jsonUpdate, UpdateStatement.class);

        assertNotNull(result);
        assertInstanceOf(UpdateResult.class, result);
        UpdateResult updateResult = (UpdateResult) result;
        assertTrue(updateResult.getAffectedRows() > 0);
    }
}