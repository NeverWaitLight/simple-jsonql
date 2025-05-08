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
    public void testDeleteWithWhere() throws Exception {
        // First insert a test record
        String randomName = "张三" + new Random().nextInt();
        String insertQuery = """
                {
                    "statement": "create",
                    "into": "user",
                    "values": {
                        "name": "%s"
                    }
                }
                """.formatted(randomName);
        engine.execute(insertQuery);

        // Then delete it
        String deleteQuery = """
                {
                    "statement": "delete",
                    "from": "user",
                    "where": {
                        "type": "comparison",
                        "field": "name",
                        "operator": "eq",
                        "value": "%s"
                    }
                }
                """.formatted(randomName);

        Object result = engine.execute(deleteQuery);
        assertNotNull(result, "Delete result should not be null");
        assertTrue(result instanceof Number, "Delete result should be a number");
        assertEquals(1, ((Number) result).intValue(), "Delete should affect 1 row");

        // Verify the record is deleted
        String selectQuery = """
                {
                    "statement": "query",
                    "select": ["name"],
                    "from": "user",
                    "where": {
                        "type": "comparison",
                        "field": "name",
                        "operator": "eq",
                        "value": "%s"
                    }
                }
                """.formatted(randomName);

        Object selectResult = engine.execute(selectQuery);
        assertNotNull(selectResult, "Select result should not be null");
        assertTrue(selectResult instanceof List, "Select result should be a list");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> resultList = (List<Map<String, Object>>) selectResult;
        assertTrue(resultList.isEmpty(), "Should find no records");
    }
    
    @Test
    public void testDeleteWithIds() throws Exception {
        // First insert a test record
        String randomName = "李四" + new Random().nextInt();
        String insertQuery = """
                {
                    "statement": "create",
                    "into": "user",
                    "values": {
                        "name": "%s"
                    }
                }
                """.formatted(randomName);
        Object insertResult = engine.execute(insertQuery);
        assertTrue(insertResult instanceof Number);
        int rowsInserted = ((Number) insertResult).intValue();
        assertEquals(1, rowsInserted);
        
        // Get the ID of the inserted record
        String selectQuery = """
                {
                    "statement": "query",
                    "select": ["id", "name"],
                    "from": "user",
                    "where": {
                        "type": "comparison",
                        "field": "name",
                        "operator": "eq",
                        "value": "%s"
                    }
                }
                """.formatted(randomName);
        
        Object selectResult = engine.execute(selectQuery);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> resultList = (List<Map<String, Object>>) selectResult;
        assertFalse(resultList.isEmpty());
        String id = resultList.get(0).get("id").toString();
        
        // Delete by ID
        String deleteQuery = """
                {
                    "statement": "delete",
                    "from": "user",
                    "ids": ["%s"]
                }
                """.formatted(id);
                
        Object deleteResult = engine.execute(deleteQuery);
        assertNotNull(deleteResult, "Delete result should not be null");
        assertTrue(deleteResult instanceof Number, "Delete result should be a number");
        assertEquals(1, ((Number) deleteResult).intValue(), "Delete should affect 1 row");
        
        // Verify record is gone
        Object verifyResult = engine.execute(selectQuery);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> verifyList = (List<Map<String, Object>>) verifyResult;
        assertTrue(verifyList.isEmpty(), "Record should be deleted");
    }
} 