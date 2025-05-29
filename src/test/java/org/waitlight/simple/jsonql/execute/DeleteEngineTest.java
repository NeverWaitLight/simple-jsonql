package org.waitlight.simple.jsonql.execute;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.waitlight.simple.jsonql.entity.Blog;
import org.waitlight.simple.jsonql.entity.User;
import org.waitlight.simple.jsonql.execute.result.DeleteResult;
import org.waitlight.simple.jsonql.execute.result.ExecuteResult;
import org.waitlight.simple.jsonql.execute.result.InsertResult;
import org.waitlight.simple.jsonql.metadata.MetadataSource;
import org.waitlight.simple.jsonql.statement.DeleteStatement;
import org.waitlight.simple.jsonql.statement.InsertStatement;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class DeleteEngineTest {
    private static ExecuteEngine engine;

    @BeforeAll
    public static void setUp() {
        MetadataSource metadataSource = new MetadataSource();
        metadataSource.registry(User.class);
        metadataSource.registry(Blog.class);
        engine = new ExecuteEngine(metadataSource);
    }

    @Test
    public void execute_deleteWithSingleId_returnsSuccessResult() throws Exception {
        String randomName = "测试用户" + new Random().nextInt(10000);
        String insertQuery = """
                {
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
        assertInstanceOf(InsertResult.class, insertResultObj);

        InsertResult insertResult = (InsertResult) insertResultObj;
        assertTrue(insertResult.getAffectedRows() > 0);
        assertFalse(insertResult.getMainIds().isEmpty());

        Long insertedId = insertResult.getMainIds().getFirst();

        String deleteQuery = """
                {
                    "appId": "123456",
                    "formId": "89757",
                    "entityId": "user",
                    "id": "%s"
                }
                """.formatted(insertedId);

        ExecuteResult deleteResultObj = engine.execute(deleteQuery, DeleteStatement.class);
        assertNotNull(deleteResultObj);
        assertInstanceOf(DeleteResult.class, deleteResultObj);

        DeleteResult deleteResult = (DeleteResult) deleteResultObj;
        assertEquals(1, deleteResult.getAffectedRows());
    }
}