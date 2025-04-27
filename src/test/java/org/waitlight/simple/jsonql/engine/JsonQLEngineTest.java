package org.waitlight.simple.jsonql.engine;

import org.junit.BeforeClass;
import org.junit.Test;
import org.waitlight.simple.jsonql.entity.Blog;
import org.waitlight.simple.jsonql.entity.User;
import org.waitlight.simple.jsonql.metadata.MetadataSources;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.*;

public class JsonQLEngineTest {
    private static JsonQLEngine engine;

    @BeforeClass
    public static void setUp() {
        MetadataSources metadataSources = new MetadataSources();
        metadataSources.addAnnotatedClass(User.class);
        metadataSources.addAnnotatedClass(Blog.class);
        engine = new JsonQLEngine(metadataSources);
    }

    @Test
    public void insertTest() throws Exception {
        String randomName = "张三" + new Random().nextInt();
        String jsonQuery = """
                {
                    "statement": "insert",
                    "into": "user",
                    "values": {
                        "name": "%s"
                    }
                }
                """.formatted(randomName);

        Object result = engine.execute(jsonQuery);
        assertNotNull("Insert result should not be null", result);
        assertTrue("Insert result should be a number", result instanceof Number);
        assertEquals("Insert should affect 1 row", 1, ((Number) result).intValue());

        // Verify the inserted data
        String selectQuery = """
                {
                    "statement": "SELECT",
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
        assertNotNull("Select result should not be null", selectResult);
        assertTrue("Select result should be a list", selectResult instanceof List);
        
        List<Map<String, Object>> resultList = (List<Map<String, Object>>) selectResult;
        assertEquals("Should find exactly one record", 1, resultList.size());
        assertEquals("Name should match", randomName, resultList.get(0).get("name"));
    }

    @Test
    public void deleteTest() throws Exception {
        // First insert a test record
        String randomName = "张三" + new Random().nextInt();
        String insertQuery = """
                {
                    "statement": "insert",
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
        assertNotNull("Delete result should not be null", result);
        assertTrue("Delete result should be a number", result instanceof Number);
        assertEquals("Delete should affect 1 row", 1, ((Number) result).intValue());

        // Verify the record is deleted
        String selectQuery = """
                {
                    "statement": "SELECT",
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
        assertNotNull("Select result should not be null", selectResult);
        assertTrue("Select result should be a list", selectResult instanceof List);
        
        List<Map<String, Object>> resultList = (List<Map<String, Object>>) selectResult;
        assertTrue("Should find no records", resultList.isEmpty());
    }

    @Test
    public void selectSingleTable() throws Exception {
        String jsonQuery = """
                {
                    "statement": "SELECT",
                    "select": ["name"],
                    "from": "user"
                }
                """;

        Object result = engine.execute(jsonQuery);
        assertNotNull("Select result should not be null", result);
        assertTrue("Select result should be a list", result instanceof List);
        
        List<Map<String, Object>> resultList = (List<Map<String, Object>>) result;
        assertFalse("Result list should not be empty", resultList.isEmpty());
        
        // Verify each record has the expected structure
        for (Map<String, Object> record : resultList) {
            assertTrue("Each record should have a name field", record.containsKey("name"));
            assertNotNull("Name field should not be null", record.get("name"));
        }
    }

    @Test
    public void selectAndJoin() throws Exception {
        String jsonQuery = """
                {
                    "statement": "SELECT",
                    "select": ["name", "blogs"],
                    "from": "user"
                }
                """;

        Object result = engine.execute(jsonQuery);
        assertNotNull("Select result should not be null", result);
        assertTrue("Select result should be a list", result instanceof List);
        
        List<Map<String, Object>> resultList = (List<Map<String, Object>>) result;
        assertFalse("Result list should not be empty", resultList.isEmpty());
        
        // Verify each record has the expected structure
        for (Map<String, Object> record : resultList) {
            assertTrue("Each record should have a name field", record.containsKey("name"));
            assertTrue("Each record should have a blogs field", record.containsKey("blogs"));
            assertNotNull("Name field should not be null", record.get("name"));
            assertTrue("Blogs field should be a list", record.get("blogs") instanceof List);
        }
    }
}