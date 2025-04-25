package org.waitlight.simple.jsonql;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.waitlight.simple.jsonql.entity.Blog;
import org.waitlight.simple.jsonql.entity.User;

import java.util.List;
import java.util.Map;

public class JsonQLEngineTest {
    @Test
    public void selectSingleTable() throws Exception {
        String jsonQuery = """
                {
                    "statement": "SELECT",
                    "select": ["name"],
                    "from": "user"
                }
                """;

        MetadataSources metadataSources = new MetadataSources();
        metadataSources.addAnnotatedClass(User.class);
        metadataSources.addAnnotatedClass(Blog.class);

        JsonQLEngine engine = new JsonQLEngine(metadataSources);
        List<Map<String, Object>> result = engine.execute(jsonQuery);
        ObjectMapper objectMapper = new ObjectMapper();
        System.out.println(objectMapper.writeValueAsString(result));
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

        MetadataSources metadataSources = new MetadataSources();
        metadataSources.addAnnotatedClass(User.class);
        metadataSources.addAnnotatedClass(Blog.class);

        JsonQLEngine engine = new JsonQLEngine(metadataSources);
        List<Map<String, Object>> result = engine.execute(jsonQuery);
        ObjectMapper objectMapper = new ObjectMapper();
        System.out.println(objectMapper.writeValueAsString(result));
    }


}