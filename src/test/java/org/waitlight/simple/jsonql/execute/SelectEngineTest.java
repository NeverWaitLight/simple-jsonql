package org.waitlight.simple.jsonql.execute;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.waitlight.simple.jsonql.execute.result.ExecuteResult;
import org.waitlight.simple.jsonql.execute.result.InsertResult;
import org.waitlight.simple.jsonql.execute.result.SelectResult;
import org.waitlight.simple.jsonql.entity.Blog;
import org.waitlight.simple.jsonql.entity.User;
import org.waitlight.simple.jsonql.metadata.MetadataSource;
import org.waitlight.simple.jsonql.statement.InsertStatement;
import org.waitlight.simple.jsonql.statement.SelectStatement;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class SelectEngineTest {
    private static ExecuteEngine engine;
    private static Long userId;
    private static Long blogId;
    private static String userName;
    private static String blogTitle;

    @BeforeAll
    public static void setUp() {
        MetadataSource metadataSource = new MetadataSource();
        metadataSource.registry(User.class);
        metadataSource.registry(Blog.class);
        engine = new ExecuteEngine(metadataSource);
    }

    @BeforeEach
    public void prepareTestData() throws Exception {
        // 为每个测试创建唯一数据
        String uniquePrefix = "Test" + new Random().nextInt(10000);
        userName = uniquePrefix + "_User";
        blogTitle = uniquePrefix + "_Blog";

        // 创建用户及其博客
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
                """.formatted(userName, blogTitle);

        ExecuteResult result = engine.execute(insertQuery, InsertStatement.class);
        assertTrue(result instanceof InsertResult);
        InsertResult insertResult = (InsertResult) result;
        userId = insertResult.getMainIds().get(0);
        blogId = insertResult.getNestedIds().get(0);
    }

    @Test
    public void testBasicQuery() throws Exception {
        // 基本分页查询，无过滤条件
        String query = """
                {
                    "statement": "select",
                    "appId": "123456",
                    "formId": "89757",
                    "entityId": "user",
                    "page": {"size": 10, "number": 1}
                }
                """;

        ExecuteResult result = engine.execute(query, SelectStatement.class);
        assertNotNull(result);
        assertTrue(result instanceof SelectResult);
        SelectResult selectResult = (SelectResult) result;

        List<Map<String, Object>> resultList = selectResult.getRecords();
        assertFalse(resultList.isEmpty(), "查询结果不应为空");

        // 验证是否获取到正确数量的记录
        assertTrue(resultList.size() > 0, "应该至少返回一条记录");
        assertTrue(selectResult.getTotalCount() > 0, "总记录数应大于0");
    }

    @Test
    public void testQueryWithFilters() throws Exception {
        // 带过滤条件的查询
        String query = """
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

        ExecuteResult result = engine.execute(query, SelectStatement.class);
        assertNotNull(result);
        assertTrue(result instanceof SelectResult);
        SelectResult selectResult = (SelectResult) result;

        List<Map<String, Object>> resultList = selectResult.getRecords();
        assertEquals(1, resultList.size(), "应该只返回一条记录");
        assertEquals(userId.toString(), resultList.get(0).get("id").toString());
        assertEquals(userName, resultList.get(0).get("name"));
    }

    @Test
    public void testQueryWithSort() throws Exception {
        // 排序查询
        String query = """
                {
                    "statement": "select",
                    "appId": "123456",
                    "formId": "89757",
                    "entityId": "user",
                    "sort": [
                        {"field": "id", "direction": "DESC"}
                    ],
                    "page": {"size": 10, "number": 1}
                }
                """;

        ExecuteResult result = engine.execute(query, SelectStatement.class);
        assertNotNull(result);
        assertTrue(result instanceof SelectResult);
        SelectResult selectResult = (SelectResult) result;

        List<Map<String, Object>> resultList = selectResult.getRecords();
        assertFalse(resultList.isEmpty(), "查询结果不应为空");

        // 检查是否按ID降序排列
        if (resultList.size() >= 2) {
            Long firstId = Long.parseLong(resultList.get(0).get("id").toString());
            Long secondId = Long.parseLong(resultList.get(1).get("id").toString());
            assertTrue(firstId > secondId, "第一条记录的ID应该大于第二条记录");
        }
    }

    @Test
    public void testSubTableQuery() throws Exception {
        // 子表查询（通过关联字段查询）
        String query = """
                {
                    "statement": "select",
                    "appId": "123456",
                    "formId": "89757",
                    "entityId": "user",
                    "relations": [
                        {"field": "blogs", "alias": "blogs"}
                    ],
                    "filters": {
                        "rel": "and",
                        "conditions": [
                            {"field": "name", "method": "eq", "value": "%s"}
                        ]
                    },
                    "sort": [
                        {"field": "name", "direction": "ASC"}
                    ],
                    "page": {"size": 10, "number": 1}
                }
                """.formatted(userName);

        ExecuteResult result = engine.execute(query, SelectStatement.class);
        assertNotNull(result);
        assertTrue(result instanceof SelectResult);
        SelectResult selectResult = (SelectResult) result;

        List<Map<String, Object>> resultList = selectResult.getRecords();
        assertFalse(resultList.isEmpty(), "查询结果应包含记录");

        boolean found = false;
        for (Map<String, Object> row : resultList) {
            if (userId.toString().equals(row.get("id").toString())) {
                found = true;
                assertEquals(userName, row.get("name"));
                break;
            }
        }
        assertTrue(found, "查询结果应包含测试用户");
    }
}