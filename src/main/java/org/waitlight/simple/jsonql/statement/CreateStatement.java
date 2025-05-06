package org.waitlight.simple.jsonql.statement;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.waitlight.simple.jsonql.statement.model.NestedEntity;

@Data
@EqualsAndHashCode(callSuper = true)
public class CreateStatement extends NestedEntity {
    @JsonProperty("into")
    private String into;
    
    @JsonProperty("values")
    private Object values;
}