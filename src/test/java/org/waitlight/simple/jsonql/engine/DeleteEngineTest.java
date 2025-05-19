package org.waitlight.simple.jsonql.engine;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.waitlight.simple.jsonql.engine.result.DeleteResult;
import org.waitlight.simple.jsonql.engine.result.ExecuteResult;
import org.waitlight.simple.jsonql.engine.result.InsertResult;
import org.waitlight.simple.jsonql.entity.Blog;
import org.waitlight.simple.jsonql.entity.User;
import org.waitlight.simple.jsonql.metadata.MetadataSource;
import org.waitlight.simple.jsonql.statement.DeleteStatement;
import org.waitlight.simple.jsonql.statement.InsertStatement;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class DeleteEngineTest {
    private static JsonQLEngine engine;

    @BeforeAll
    public static void setUp() {
        MetadataSource metadataSource = new MetadataSource();
        metadataSource.addAnnotatedClass(User.class);
        metadataSource.addAnnotatedClass(Blog.class);
        engine = new JsonQLEngine(metadataSource);
    }

    @Test
    public void testDeleteWithIds() throws Exception {
        String randomName = "真实ID李四" + new Random().nextInt();
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
        assertNotNull(insertResultObj);
        assertTrue(insertResultObj instanceof InsertResult, "Result should be InsertResult");

        InsertResult insertResult = (InsertResult) insertResultObj;
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

        ExecuteResult deleteResultObj = engine.execute(deleteQuery, DeleteStatement.class);
        assertNotNull(deleteResultObj, "Delete result should not be null");
        assertTrue(deleteResultObj instanceof DeleteResult, "Result should be DeleteResult");

        DeleteResult deleteResult = (DeleteResult) deleteResultObj;
        assertEquals(1, deleteResult.getAffectedRows(), "Delete should affect 1 row when using the actual ID");
    }
}