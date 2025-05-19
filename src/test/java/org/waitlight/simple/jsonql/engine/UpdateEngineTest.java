package org.waitlight.simple.jsonql.engine;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.waitlight.simple.jsonql.engine.result.ExecuteResult;
import org.waitlight.simple.jsonql.engine.result.InsertResult;
import org.waitlight.simple.jsonql.engine.result.SelectResult;
import org.waitlight.simple.jsonql.engine.result.UpdateResult;
import org.waitlight.simple.jsonql.entity.Blog;
import org.waitlight.simple.jsonql.entity.User;
import org.waitlight.simple.jsonql.metadata.MetadataSources;
import org.waitlight.simple.jsonql.statement.InsertStatement;
import org.waitlight.simple.jsonql.statement.SelectStatement;
import org.waitlight.simple.jsonql.statement.UpdateStatement;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class UpdateEngineTest {
    private static JsonQLEngine engine;

    @BeforeAll
    public static void setUp() {
        MetadataSources metadataSources = new MetadataSources();
        metadataSources.addAnnotatedClass(User.class);
        metadataSources.addAnnotatedClass(Blog.class);
        engine = new JsonQLEngine(metadataSources);
    }

    @Test
    public void testUpdateSingleEntity() throws Exception {
        // 1. 先创建一条记录
        String randomName = "原始名称" + new Random().nextInt(10000);
        String updatedName = "已更新名称" + new Random().nextInt(10000);

        String insertQuery = """
                {
                    "statement": "insert",
                    "appId": "123456",
                    "formId": "89757",
                    "entityId": "user",
                    "fields": [
                        {"field": "name", "value": "%s"}
                    ]
                }
                """.formatted(randomName);

        ExecuteResult insertResultObj = engine.execute(insertQuery, InsertStatement.class);
        assertTrue(insertResultObj instanceof InsertResult);
        InsertResult insertResult = (InsertResult) insertResultObj;
        assertTrue(insertResult.getAffectedRows() > 0);
        assertFalse(insertResult.getMainIds().isEmpty());

        Long userId = insertResult.getMainIds().get(0);

        // 2. 更新该记录
        String updateQuery = """
                {
                    "statement": "update",
                    "appId": "123456",
                    "formId": "89757",
                    "entityId": "user",
                    "dataId": "%s",
                    "fields": [
                        {"field": "name", "value": "%s"}
                    ]
                }
                """.formatted(userId, updatedName);

        ExecuteResult updateResultObj = engine.execute(updateQuery, UpdateStatement.class);
        assertNotNull(updateResultObj);
        assertTrue(updateResultObj instanceof UpdateResult);
        UpdateResult updateResult = (UpdateResult) updateResultObj;
        assertTrue(updateResult.getAffectedRows() > 0, "Update should affect at least one row");

        // 3. 验证更新是否成功
        String selectQuery = """
                {
                    "statement": "select",
                    "appId": "123456",
                    "formId": "89757",
                    "entityId": "user",
                    "filters": {
                        "rel": "and",
                        "conditions": [
                            {"field": "id", "method": "eq", "value": %s}
                        ]
                    },
                    "page": {"size": 10, "number": 1}
                }
                """.formatted(userId);

        ExecuteResult selectResultObj = engine.execute(selectQuery, SelectStatement.class);
        assertNotNull(selectResultObj);
        assertTrue(selectResultObj instanceof SelectResult);
        SelectResult selectResult = (SelectResult) selectResultObj;

        List<Map<String, Object>> resultList = selectResult.getRecords();
        assertFalse(resultList.isEmpty(), "Updated record should exist");
        assertEquals(updatedName, resultList.get(0).get("name"),
                "Name should be updated to new value");
    }

    @Test
    public void testUpdateWithNestedEntity() throws Exception {
        // 1. 先创建一条带嵌套实体的记录
        String randomName = "用户" + new Random().nextInt(10000);
        String originalTitle = "原始标题" + new Random().nextInt(10000);
        String updatedTitle = "更新后标题" + new Random().nextInt(10000);

        String insertQuery = """
                {
                    "statement": "insert",
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
                """.formatted(randomName, originalTitle);

        ExecuteResult insertResultObj = engine.execute(insertQuery, InsertStatement.class);
        assertTrue(insertResultObj instanceof InsertResult);
        InsertResult insertResult = (InsertResult) insertResultObj;
        assertTrue(insertResult.getAffectedRows() > 0);
        assertFalse(insertResult.getMainIds().isEmpty());
        assertFalse(insertResult.getNestedIds().isEmpty());

        Long userId = insertResult.getMainIds().get(0);
        Long blogId = insertResult.getNestedIds().get(0);

        // 2. 更新嵌套实体 - 直接更新博客
        String updateQuery = """
                {
                    "statement": "update",
                    "appId": "123456",
                    "formId": "89758",
                    "entityId": "blog",
                    "dataId": "%s",
                    "fields": [
                        {"field": "title", "value": "%s"}
                    ]
                }
                """.formatted(blogId, updatedTitle);

        ExecuteResult updateResultObj = engine.execute(updateQuery, UpdateStatement.class);
        assertNotNull(updateResultObj);
        assertTrue(updateResultObj instanceof UpdateResult);
        UpdateResult updateResult = (UpdateResult) updateResultObj;
        assertTrue(updateResult.getAffectedRows() > 0, "Update should affect at least one row");

        // 3. 验证更新是否成功
        String selectQuery = """
                {
                    "statement": "select",
                    "appId": "123456",
                    "formId": "89758",
                    "entityId": "blog",
                    "filters": {
                        "rel": "and",
                        "conditions": [
                            {"field": "id", "method": "eq", "value": %s}
                        ]
                    },
                    "page": {"size": 10, "number": 1}
                }
                """.formatted(blogId);

        ExecuteResult selectResultObj = engine.execute(selectQuery, SelectStatement.class);
        assertNotNull(selectResultObj);
        assertTrue(selectResultObj instanceof SelectResult);
        SelectResult selectResult = (SelectResult) selectResultObj;

        List<Map<String, Object>> resultList = selectResult.getRecords();
        assertFalse(resultList.isEmpty(), "Updated blog record should exist");
        assertEquals(updatedTitle, resultList.get(0).get("title"),
                "Blog title should be updated to new value");
    }
}