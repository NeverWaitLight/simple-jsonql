package org.waitlight.simple.jsonql.engine;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.waitlight.simple.jsonql.entity.Blog;
import org.waitlight.simple.jsonql.entity.User;
import org.waitlight.simple.jsonql.metadata.MetadataSources;

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
        Object insertResultObj = engine.execute(insertQuery);
        assertNotNull(insertResultObj);

        InsertExecutionResult insertResult = (InsertExecutionResult) insertResultObj;
        assertTrue(insertResult.getAffectedRows() > 0, "Insert should affect at least one row");
        assertFalse(insertResult.getMainIds().isEmpty(), "Insert should return at least one ID");

        Long insertedId = insertResult.getMainIds().get(0);
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