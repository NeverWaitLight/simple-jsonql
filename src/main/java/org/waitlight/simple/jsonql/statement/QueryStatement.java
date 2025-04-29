package org.waitlight.simple.jsonql.statement;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.waitlight.simple.jsonql.statement.model.*;

import java.util.List;

@Getter
@Setter
public class QueryStatement extends JsonQLStatement {
    @JsonProperty("appId")
    private String appId;
    
    @JsonProperty("formId")
    private String formId;
    
    @JsonProperty("entityId")
    private String entityId;
    
    @JsonProperty("filters")
    private Filter filters;

    @JsonProperty("sort")
    private List<Sort> sort;

    @JsonProperty("page")
    private Page page;

    public QueryStatement() {
        setStatement(StatementType.QUERY);
    }
}