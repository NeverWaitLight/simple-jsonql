package org.waitlight.simple.jsonql.engine;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.waitlight.simple.jsonql.entity.Blog;
import org.waitlight.simple.jsonql.entity.User;
import org.waitlight.simple.jsonql.metadata.MetadataSources;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class DeleteEngineTest {
    private static JsonQLEngine engine;

    @BeforeAll
    public static void setUp() {
        MetadataSources metadataSources = new MetadataSources();
        metadataSources.addAnnotatedClass(User.class);
        metadataSources.addAnnotatedClass(Blog.class);
        engine = new JsonQLEngine(metadataSources);
    }

    @Test
    public void testDeleteWithIds() throws Exception {
        String randomName = "真实ID李四" + new Random().nextInt();
        String insertQuery = """
                {
                    "statement": "create",
                    "appId": "123456",
                    "formId": "89757",
                    "entityId": "user",
                    "fields": [
                        {"field": "name", "value": "%s"}
                    ]
                }
                """.formatted(randomName);
        Object insertResult = engine.execute(insertQuery);
        assertNotNull(insertResult);

        Long insertedId;
        if (insertResult instanceof InsertExecutionResult) {
            InsertExecutionResult result = (InsertExecutionResult) insertResult;
            assertTrue(result.getAffectedRows() > 0, "Insert should affect at least one row");
            assertFalse(result.getMainIds().isEmpty(), "Insert should return at least one ID");
            insertedId = result.getMainIds().get(0);
        } else {
            // 兼容旧版返回值类型
            assertTrue(insertResult instanceof Number && ((Number) insertResult).intValue() > 0,
                    "Insert should affect at least one row");

            // 通过查询获取ID
            String selectQueryForId = """
                    {
                        "statement": "query",
                        "select": ["id"],
                        "from": "user",
                        "where": {
                            "type": "comparison",
                            "field": "name",
                            "operator": "eq",
                            "value": "%s"
                        }
                    }
                    """.formatted(randomName);

            Object selectResult = engine.execute(selectQueryForId);
            assertNotNull(selectResult, "Select result for ID should not be null");
            assertTrue(selectResult instanceof List, "Select result should be a list");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> resultList = (List<Map<String, Object>>) selectResult;
            assertFalse(resultList.isEmpty(), "Inserted record should be found to get its ID");
            insertedId = Long.parseLong(resultList.get(0).get("id").toString());
        }
        
        assertNotNull(insertedId, "Inserted ID should not be null");

        String deleteQuery = """
                {
                    "statement": "delete",
                    "appId": "123456",
                    "formId": "89757",
                    "entityId": "user",
                    "ids": ["%s"]
                }
                """.formatted(insertedId);

        Object deleteResult = engine.execute(deleteQuery);
        assertNotNull(deleteResult, "Delete result should not be null");
        assertTrue(deleteResult instanceof Number, "Delete result should be a number");
        assertEquals(1, ((Number) deleteResult).intValue(), "Delete should affect 1 row when using the actual ID");
    }
}