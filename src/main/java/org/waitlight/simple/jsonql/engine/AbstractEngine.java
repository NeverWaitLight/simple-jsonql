package org.waitlight.simple.jsonql.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.waitlight.simple.jsonql.jql.JsonQL;
import org.waitlight.simple.jsonql.metadata.Metadata;
import org.waitlight.simple.jsonql.metadata.MetadataSources;

public abstract class AbstractEngine {

    protected final ObjectMapper objectMapper = new ObjectMapper();
    protected final Metadata metadata;

    public AbstractEngine(MetadataSources metadataSources) {
        this.metadata = metadataSources.buildMetadata();
    }

    public String parseSql(String jqlStr) throws Exception {
        JsonQL jql = objectMapper.readValue(jqlStr, JsonQL.class);
        return parseSql(jql);
    }

    public abstract String parseSql(JsonQL jql);
}
