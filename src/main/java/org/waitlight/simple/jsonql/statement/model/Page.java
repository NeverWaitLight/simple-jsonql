package org.waitlight.simple.jsonql.statement.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Page {
    @JsonProperty("size")
    private Integer size;

    @JsonProperty("number")
    private Integer number;
}