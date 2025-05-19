package org.waitlight.simple.jsonql.statement.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SortCriteria {
    @JsonProperty("field")
    private String field;

    @JsonProperty("direction")
    private String direction;
}