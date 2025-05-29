package org.waitlight.simple.jsonql.statement;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DeleteStatementParserTest {
    private final StatementParser parser = new StatementParser();

    @Test
    public void parse_deleteStatementWithIds_returnsValidDeleteStatement() throws JsonQLStatementException {
        String json = """
                {
                    "appId": "123456",
                    "formId": "89757",
                    "entityId": "89757",
                    "ids": [
                        "1", "2", "3"
                    ]
                }
                """;

        DeleteStatement statement = parser.parse(json, DeleteStatement.class);

        assertEquals("123456", statement.getAppId());
        assertEquals("89757", statement.getFormId());
        assertEquals("89757", statement.getEntityId());
        assertNotNull(statement.getIds());
        assertEquals(3, statement.getIds().size());
        assertEquals("1", statement.getIds().get(0));
        assertEquals("2", statement.getIds().get(1));
        assertEquals("3", statement.getIds().get(2));
    }
} 