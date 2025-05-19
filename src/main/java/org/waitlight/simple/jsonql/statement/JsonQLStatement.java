package org.waitlight.simple.jsonql.statement;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.waitlight.simple.jsonql.statement.model.StatementType;

@Getter
@Setter
public abstract class JsonQLStatement {
    private String appId;
    private String formId;
    private String entityId;

    @JsonIgnore
    private StatementType statement;
}