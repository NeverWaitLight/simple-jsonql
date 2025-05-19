package org.waitlight.simple.jsonql.statement.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class FilterCondition {
    @JsonProperty("field")
    private String field;

    @JsonProperty("method")
    private MethodType method;

    @JsonProperty("value")
    private Object value;

    @JsonProperty("values")
    private List<Object> values;
}