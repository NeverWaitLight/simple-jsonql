package org.waitlight.simple.jsonql.statement.model;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonProperty;

@Getter
@Setter
public class Filter {
    @JsonProperty("rel")
    private String rel;
    
    @JsonProperty("conditions")
    private List<Condition> conditions;
}