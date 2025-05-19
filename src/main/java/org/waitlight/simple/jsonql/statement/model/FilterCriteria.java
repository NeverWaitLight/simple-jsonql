package org.waitlight.simple.jsonql.statement.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class FilterCriteria {
    @JsonProperty("rel")
    private String rel;

    @JsonProperty("conditions")
    private List<FilterCondition> conditions;
}