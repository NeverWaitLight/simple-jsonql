package org.waitlight.simple.jsonql.statement.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.waitlight.simple.jsonql.statement.JsonQLStatement;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class NestedEntity extends JsonQLStatement {
    @JsonProperty("dataId")
    private String dataId;
    
    @JsonProperty("fields")
    private List<Field> fields;
}