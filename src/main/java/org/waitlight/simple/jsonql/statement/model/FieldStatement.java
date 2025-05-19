package org.waitlight.simple.jsonql.statement.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class FieldStatement {
    @JsonProperty("field")
    private String field;

    @JsonProperty("value")
    private Object value;

    @JsonProperty("values")
    private List<NestedStatement> values;
}