package org.waitlight.simple.jsonql.engine;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.waitlight.simple.jsonql.entity.Blog;
import org.waitlight.simple.jsonql.entity.User;
import org.waitlight.simple.jsonql.metadata.MetadataSources;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class JsonQLEngineTest {
    private static JsonQLEngine engine;

    @BeforeAll
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
        assertNotNull(result, "Insert result should not be null");
        assertTrue(result instanceof Number, "Insert result should be a number");
        assertEquals(1, ((Number) result).intValue(), "Insert should affect 1 row");

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
        assertNotNull(selectResult, "Select result should not be null");
        assertTrue(selectResult instanceof List, "Select result should be a list");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> resultList = (List<Map<String, Object>>) selectResult;
        assertEquals(1, resultList.size(), "Should find exactly one record");
        assertEquals(randomName, resultList.get(0).get("name"), "Name should match");
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
        assertNotNull(result, "Delete result should not be null");
        assertTrue(result instanceof Number, "Delete result should be a number");
        assertEquals(1, ((Number) result).intValue(), "Delete should affect 1 row");

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
        assertNotNull(selectResult, "Select result should not be null");
        assertTrue(selectResult instanceof List, "Select result should be a list");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> resultList = (List<Map<String, Object>>) selectResult;
        assertTrue(resultList.isEmpty(), "Should find no records");
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
        assertNotNull(result, "Select result should not be null");
        assertTrue(result instanceof List, "Select result should be a list");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> resultList = (List<Map<String, Object>>) result;
        assertFalse(resultList.isEmpty(), "Result list should not be empty");

        // Verify each record has the expected structure
        for (Map<String, Object> record : resultList) {
            assertTrue(record.containsKey("name"), "Each record should have a name field");
            assertNotNull(record.get("name"), "Name field should not be null");
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
        assertNotNull(result, "Select result should not be null");
        assertTrue(result instanceof List, "Select result should be a list");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> resultList = (List<Map<String, Object>>) result;
        assertFalse(resultList.isEmpty(), "Result list should not be empty");

        // Verify each record has the expected structure
        for (Map<String, Object> record : resultList) {
            assertTrue(record.containsKey("name"), "Each record should have a name field");
            assertTrue(record.containsKey("blogs"), "Each record should have a blogs field");
            assertNotNull(record.get("name"), "Name field should not be null");
            assertTrue(record.get("blogs") instanceof LinkedHashMap<?, ?>, "Blogs field should be a list");
        }
    }
}
