package org.waitlight.simple.jsonql;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.waitlight.simple.jsonql.jql.JsonQL;

public class JsonQLTest {
    @Test
    public void test() throws JsonProcessingException {
        String json = """
                {
                  "statement": "select",
                  "select": ["id", "name", "age"],
                  "from": "user",
                  "where": {
                    "age": {
                      "gt": 18,
                      "lt": 60
                    },
                    "status": "active"
                  }
                }
                """;
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.readValue(json, JsonQL.class);
    }
}
