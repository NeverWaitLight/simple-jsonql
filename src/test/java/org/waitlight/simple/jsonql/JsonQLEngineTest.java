package org.waitlight.simple.jsonql;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.waitlight.simple.jsonql.engine.JsonQLEngine;
import org.waitlight.simple.jsonql.entity.Blog;
import org.waitlight.simple.jsonql.entity.User;
import org.waitlight.simple.jsonql.metadata.MetadataSources;

import java.util.Random;

public class JsonQLEngineTest {

    @Test
    public void insertTest() throws Exception {
        String jsonQuery = """
                {
                    "statement": "insert",
                    "into": "user",
                    "values": {
                        "name": "张三%s"
                    }
                }
                """;
        jsonQuery = jsonQuery.formatted(new Random().nextInt());

        MetadataSources metadataSources = new MetadataSources();
        metadataSources.addAnnotatedClass(User.class);
        metadataSources.addAnnotatedClass(Blog.class);

        JsonQLEngine engine = new JsonQLEngine(metadataSources);
        Object result = engine.execute(jsonQuery);
        ObjectMapper objectMapper = new ObjectMapper();
        System.out.println(objectMapper.writeValueAsString(result));
    }

    @Test
    public void deleteTest() throws Exception {
        String jsonQuery = """
                {
                    "statement": "delete",
                    "into": "user",
                    "where": {
                        "name": {
                            "like": "张三"
                        }
                    }
                }
                """;
        jsonQuery = jsonQuery.formatted(new Random().nextInt());

        MetadataSources metadataSources = new MetadataSources();
        metadataSources.addAnnotatedClass(User.class);
        metadataSources.addAnnotatedClass(Blog.class);

        JsonQLEngine engine = new JsonQLEngine(metadataSources);
        Object result = engine.execute(jsonQuery);
        ObjectMapper objectMapper = new ObjectMapper();
        System.out.println(objectMapper.writeValueAsString(result));
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

        MetadataSources metadataSources = new MetadataSources();
        metadataSources.addAnnotatedClass(User.class);
        metadataSources.addAnnotatedClass(Blog.class);

        JsonQLEngine engine = new JsonQLEngine(metadataSources);
        Object result = engine.execute(jsonQuery);
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
        Object result = engine.execute(jsonQuery);
        ObjectMapper objectMapper = new ObjectMapper();
        System.out.println(objectMapper.writeValueAsString(result));
    }


}