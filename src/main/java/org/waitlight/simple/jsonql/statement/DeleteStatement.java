package org.waitlight.simple.jsonql.statement;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

import org.waitlight.simple.jsonql.statement.model.JsonQLStatement;
import org.waitlight.simple.jsonql.statement.model.WhereCondition;

@Data
@EqualsAndHashCode(callSuper = true)
public class DeleteStatement extends JsonQLStatement {
    @JsonProperty("where")
    private WhereCondition where;
    
    @JsonProperty("ids")
    private List<String> ids;
} 